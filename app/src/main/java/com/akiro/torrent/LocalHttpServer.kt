package com.akiro.torrent

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.*
import java.net.URLDecoder

/**
 * LocalHttpServer: serves files from a directory and supports basic byte range requests so ExoPlayer can stream partial files.
 * Uses NanoHTTPD (fi.iki.elonen:nanohttpd) — add dependency if you choose to compile.
 */
class LocalHttpServer(port: Int, private val baseDir: File) : NanoHTTPD(port) {
    private val TAG = "LocalHttpServer"
    var listeningPort: Int = 0

    override fun start() {
        super.start(SOCKET_READ_TIMEOUT, false)
        // NanoHTTPD assigns the listening port; expose it
        listeningPort = this.listeningPort
        Log.i(TAG, "LocalHttpServer started on port $listeningPort; serving: ${baseDir.absolutePath}")
    }

    override fun serve(session: IHTTPSession): Response {
        try {
            val uri = session.uri.removePrefix("/")
            val decoded = URLDecoder.decode(uri, "UTF-8")
            val target = File(baseDir, decoded)
            if (!target.exists() || !target.isFile) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
            }

            val total = target.length()
            val range = session.headers["range"]
            if (range != null) {
                // parse Range: bytes=START-
                val parts = range.replace("bytes=", "").split("-")
                val start = parts[0].toLongOrNull() ?: 0L
                val end = if (parts.size > 1 && parts[1].isNotEmpty()) parts[1].toLong() else total - 1
                val contentLength = end - start + 1
                val fis = RandomAccessFile(target, "r")
                fis.seek(start)
                val buffer = ByteArray(8192)
                val baos = ByteArrayOutputStream()
                var remaining = contentLength
                while (remaining > 0) {
                    val read = fis.read(buffer, 0, if (remaining > buffer.size) buffer.size else remaining.toInt())
                    if (read <= 0) break
                    baos.write(buffer, 0, read)
                    remaining -= read
                }
                fis.close()
                val mime = getMimeTypeForFile(target.name)
                val res = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mime, ByteArrayInputStream(baos.toByteArray()), contentLength)
                res.addHeader("Accept-Ranges", "bytes")
                res.addHeader("Content-Range", "bytes $start-$end/$total")
                return res
            } else {
                val fis = FileInputStream(target)
                val mime = getMimeTypeForFile(target.name)
                return newChunkedResponse(Response.Status.OK, mime, fis)
            }
        } catch (e: Exception) {
            Log.e("LocalHttpServer", "serve error", e)
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server error")
        }
    }
}
