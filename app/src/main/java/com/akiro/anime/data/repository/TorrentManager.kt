package com.akiro.anime.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.libtorrent4j.Priority
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentFlags
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import java.io.File
import java.io.RandomAccessFile
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * In-app BitTorrent -> HTTP bridge.
 *
 * The player never receives a magnet URI directly. Instead:
 *  1. libtorrent resolves the magnet and downloads only video files;
 *  2. a loopback HTTP server exposes the selected file;
 *  3. Media3/ExoPlayer plays the localhost URL and can issue HTTP Range requests.
 *
 * This is the same architectural boundary needed by a Stremio-style player.
 */
class TorrentManager(private val context: Context) {

    private val session = SessionManager()
    private var started = false
    private var handle: TorrentHandle? = null
    private var selectedFile: File? = null
    private var server: LocalTorrentHttpServer? = null
    private var downloadDir: File? = null
    private var progressThread: Thread? = null

    @Volatile
    private var stopped = false

    fun start() {
        if (started) return
        session.start()
        try {
            session.startDht()
        } catch (_: Throwable) {
            // Some libtorrent builds start DHT as part of SessionManager.start().
        }
        started = true
    }

    suspend fun streamFromMagnet(
        magnetLink: String,
        onReady: (String) -> Unit,
        onError: (Throwable) -> Unit,
        onProgress: (Float) -> Unit = {}
    ) {
        withContext(Dispatchers.IO) {
            try {
                require(magnetLink.startsWith("magnet:")) { "A fonte não é um magnet URI." }
                stopped = false
                start()

                val dir = File(context.cacheDir, "torrent_streams")
                if (!dir.exists()) dir.mkdirs()
                downloadDir = dir

                // Fetch metadata first. This avoids adding every file in a multi-file
                // torrent before we know which one is the video.
                val metadata = session.fetchMagnet(magnetLink, 45, dir)
                    ?: error("Não foi possível obter os metadados do torrent.")

                val info = TorrentInfo.bdecode(metadata)
                require(info.isValid) { "Metadados do torrent inválidos." }

                val priorities = Array(info.numFiles()) { Priority.IGNORE }
                var videoIndex = -1

                for (i in 0 until info.numFiles()) {
                    val path = info.files().filePath(i)
                    if (isVideo(path)) {
                        // Prefer the largest video when a torrent contains multiple
                        // video files (opening/ending extras, samples, etc.).
                        if (videoIndex == -1 ||
                            info.files().fileSize(i) > info.files().fileSize(videoIndex)
                        ) {
                            videoIndex = i
                        }
                    }
                }

                require(videoIndex >= 0) {
                    "O torrent não contém um arquivo de vídeo compatível."
                }

                priorities[videoIndex] = Priority.DEFAULT

                // Add only the selected video to the download queue.
                session.download(
                    info,
                    dir,
                    null,
                    priorities,
                    null,
                    TorrentFlags.SEQUENTIAL_DOWNLOAD
                )

                val infoHash = info.infoHash()
                var torrent: TorrentHandle? = null

                repeat(100) {
                    if (stopped) return@withContext
                    torrent = try { session.find(infoHash) } catch (_: Throwable) { null }
                    if (torrent?.isValid == true) return@repeat
                    Thread.sleep(100)
                }

                val th = torrent ?: error("Não foi possível iniciar o torrent.")
                require(th.isValid) { "Torrent inválido." }

                handle = th

                val relativePath = info.files().filePath(videoIndex)
                val video = File(dir, relativePath)
                video.parentFile?.mkdirs()
                selectedFile = video

                // Start the local HTTP bridge before notifying the player.
                val localServer = LocalTorrentHttpServer(video, th, videoIndex, info.files().fileSize(videoIndex))
                localServer.start()
                server = localServer

                onReady(localServer.url())
                progressThread?.interrupt()
                progressThread = Thread({
                    while (!stopped && th.isValid) {
                        try {
                            val progress = th.fileProgress().getOrNull(videoIndex)?.toFloat() ?: 0f
                            val total = info.files().fileSize(videoIndex).coerceAtLeast(1L).toFloat()
                            onProgress((progress / total).coerceIn(0f, 1f))
                            if (progress >= total) break
                            Thread.sleep(350)
                        } catch (_: Throwable) {
                            break
                        }
                    }
                }, "Akiro-TorrentProgress").apply { isDaemon = true; start() }
            } catch (t: Throwable) {
                stop()
                onError(t)
            }
        }
    }

    fun stop() {
        stopped = true
        progressThread?.interrupt()
        progressThread = null
        server?.stop()
        server = null
        selectedFile = null

        val th = handle
        handle = null
        if (th != null) {
            try { session.remove(th) } catch (_: Throwable) {}
        }
    }

    fun release() {
        stop()
        if (started) {
            try { session.stop() } catch (_: Throwable) {}
            started = false
        }
    }

    private fun isVideo(path: String): Boolean {
        val p = path.lowercase(Locale.US)
        return p.endsWith(".mp4") ||
            p.endsWith(".mkv") ||
            p.endsWith(".webm") ||
            p.endsWith(".avi") ||
            p.endsWith(".mov") ||
            p.endsWith(".m4v")
    }

    /**
     * Small HTTP/1.1 loopback server with Range support.
     * It is intentionally bound to 127.0.0.1 and is not reachable from LAN/WAN.
     */
    private class LocalTorrentHttpServer(
        private val file: File,
        private val torrent: TorrentHandle,
        private val fileIndex: Int,
        private val totalSize: Long,
    ) {
        private val running = AtomicBoolean(false)
        private var socket: ServerSocket? = null
        private var worker: Thread? = null

        fun start() {
            socket = ServerSocket(0, 8, java.net.InetAddress.getByName("127.0.0.1"))
            running.set(true)
            worker = Thread({
                while (running.get()) {
                    try {
                        val client = socket?.accept() ?: break
                        Thread { serve(client) }.start()
                    } catch (_: SocketException) {
                        break
                    } catch (_: Throwable) {
                        if (!running.get()) break
                    }
                }
            }, "Akiro-TorrentHttpServer").apply {
                isDaemon = true
                start()
            }
        }

        fun url(): String =
            "http://localhost:${socket?.localPort ?: error("Servidor não iniciado")}/stream"

        fun stop() {
            running.set(false)
            try { socket?.close() } catch (_: Throwable) {}
            socket = null
        }

        private fun serve(client: Socket) {
            client.use { socket ->
                socket.soTimeout = 15_000
                val input = socket.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                val output = socket.getOutputStream().buffered()

                val requestLine = input.readLine() ?: return
                val headers = HashMap<String, String>(8)
                while (true) {
                    val line = input.readLine() ?: break
                    if (line.isEmpty()) break
                    val idx = line.indexOf(':')
                    if (idx > 0) {
                        headers[line.substring(0, idx).trim().lowercase(Locale.US)] =
                            line.substring(idx + 1).trim()
                    }
                }

                if (!requestLine.startsWith("GET ") && !requestLine.startsWith("HEAD ")) {
                    writeResponse(output, "405 Method Not Allowed", "text/plain", 0, null, false)
                    return
                }

                // HEAD is useful to ExoPlayer for probing duration/length. It must
                // never wait for the complete torrent.
                val length = totalSize.takeIf { it > 0 } ?: waitForFile()
                if (length <= 0L) {
                    writeResponse(output, "503 Service Unavailable", "text/plain", 0, null, false)
                    return
                }
                if (requestLine.startsWith("HEAD ")) {
                    val mime = mimeType(file.name)
                    val extra = "Content-Length: $length\r\nAccept-Ranges: bytes\r\nContent-Type: $mime\r\nConnection: close\r\n"
                    output.write("HTTP/1.1 200 OK\r\n$extra\r\n".toByteArray(Charsets.ISO_8859_1))
                    output.flush()
                    return
                }

                val range = parseRange(headers["range"], length)
                val requestedStart = range?.first ?: 0L
                val requestedEnd = range?.second ?: minOf(length - 1, requestedStart + 4L * 1024L * 1024L - 1L)
                val available = waitForAvailable(requestedStart, minOf(requestedEnd + 1, length), 20_000)
                if (available <= requestedStart) {
                    writeResponse(output, "503 Service Unavailable", "text/plain", 0, null, false)
                    return
                }
                val start = requestedStart
                val end = minOf(requestedEnd, available - 1)
                val contentLength = end - start + 1
                val partial = range != null || end < length - 1

                val mime = mimeType(file.name)
                val status = if (partial) "206 Partial Content" else "200 OK"
                val extra = buildString {
                    append("Content-Length: $contentLength\r\n")
                    append("Accept-Ranges: bytes\r\n")
                    append("Content-Type: $mime\r\n")
                    if (partial) append("Content-Range: bytes $start-$end/$length\r\n")
                    append("Cache-Control: no-cache\r\n")
                    append("Connection: close\r\n")
                }

                output.write("HTTP/1.1 $status\r\n$extra\r\n".toByteArray(Charsets.ISO_8859_1))
                output.flush()

                RandomAccessFile(file, "r").use { raf ->
                    raf.seek(start)
                    val buffer = ByteArray(64 * 1024)
                    var remaining = contentLength

                    while (remaining > 0 && running.get()) {
                        val requested = minOf(buffer.size.toLong(), remaining).toInt()
                        val read = readWhenDataIsAvailable(raf, buffer, requested)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        output.flush()
                        remaining -= read
                    }
                }
            }
        }

        private fun waitForFile(): Long {
            repeat(100) {
                if (!running.get()) return 0L
                if (file.exists() && file.length() > 0) return file.length()
                Thread.sleep(100)
            }
            return if (file.exists()) file.length() else 0L
        }

        private fun waitForAvailable(start: Long, requestedEndExclusive: Long, timeoutMs: Long): Long {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (running.get() && System.currentTimeMillis() < deadline) {
                try {
                    val downloaded = torrent.fileProgress().getOrNull(fileIndex) ?: 0L
                    if (downloaded > start) return minOf(downloaded, requestedEndExclusive)
                } catch (_: Throwable) {
                    // The torrent may be briefly invalid while shutting down.
                }
                Thread.sleep(120)
            }
            return try {
                minOf(torrent.fileProgress().getOrNull(fileIndex) ?: 0L, requestedEndExclusive)
            } catch (_: Throwable) { 0L }
        }

        private fun readWhenDataIsAvailable(
            raf: RandomAccessFile,
            buffer: ByteArray,
            requested: Int
        ): Int {
            return try {
                raf.read(buffer, 0, requested)
            } catch (_: java.io.EOFException) {
                0
            }
        }

        private fun parseRange(value: String?, length: Long): Pair<Long, Long>? {
            if (value == null || !value.startsWith("bytes=")) return null
            val spec = value.removePrefix("bytes=").split(",").firstOrNull()?.trim() ?: return null
            val dash = spec.indexOf('-')
            if (dash < 0) return null

            return try {
                val startPart = spec.substring(0, dash)
                val endPart = spec.substring(dash + 1)
                if (startPart.isBlank()) {
                    val suffix = endPart.toLong()
                    Pair((length - suffix).coerceAtLeast(0), length - 1)
                } else {
                    val start = startPart.toLong()
                    val end = if (endPart.isBlank()) length - 1 else endPart.toLong()
                    if (start >= length) null else Pair(start, end.coerceAtMost(length - 1))
                }
            } catch (_: NumberFormatException) {
                null
            }
        }

        private fun writeResponse(
            out: java.io.BufferedOutputStream,
            status: String,
            contentType: String,
            length: Long,
            body: ByteArray?,
            includeBody: Boolean
        ) {
            val bytes = body ?: ByteArray(0)
            out.write(
                "HTTP/1.1 $status\r\nContent-Type: $contentType\r\nContent-Length: $length\r\nConnection: close\r\n\r\n"
                    .toByteArray(Charsets.ISO_8859_1)
            )
            if (includeBody && bytes.isNotEmpty()) out.write(bytes)
            out.flush()
        }

        private fun mimeType(name: String): String = when {
            name.endsWith(".mp4", true) || name.endsWith(".m4v", true) -> "video/mp4"
            name.endsWith(".webm", true) -> "video/webm"
            name.endsWith(".mkv", true) -> "video/x-matroska"
            name.endsWith(".mov", true) -> "video/quicktime"
            else -> "application/octet-stream"
        }
    }
}
