package com.akiro.torrent

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder

/**
 * TorrentEngine: manages a jlibtorrent SessionManager to download a torrent/magnet and exposes a local HTTP URL
 * that can be played by ExoPlayer. This implementation is defensive: if the jlibtorrent classes are not available
 * at runtime (library not included), the methods will fail gracefully and log an error.
 *
 * Note: You must add jlibtorrent native .so files for each ABI and the Java wrapper dependency in Gradle.
 * See docs/torrentio-integration.md for detailed installation steps.
 */
class TorrentEngine(private val context: Context) {
    private val TAG = "TorrentEngine"

    // Lazily created session manager — reflection-friendly to avoid hard crashes if jlibtorrent is absent
    private var sessionManager: Any? = null

    private fun ensureSessionStarted() {
        if (sessionManager != null) return
        try {
            val clazz = Class.forName("com.frostwire.jlibtorrent.SessionManager")
            val ctor = clazz.getConstructor()
            val inst = ctor.newInstance()
            val startMethod = clazz.getMethod("start")
            startMethod.invoke(inst)
            sessionManager = inst
            Log.i(TAG, "jlibtorrent SessionManager started")
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "jlibtorrent not available on classpath", e)
            sessionManager = null
        } catch (e: Exception) {
            Log.e(TAG, "failed to start jlibtorrent", e)
            sessionManager = null
        }
    }

    suspend fun startTorrentAndGetLocalUrl(magnetUri: String): String? = withContext(Dispatchers.IO) {
        ensureSessionStarted()
        if (sessionManager == null) return@withContext null

        try {
            // Build save path
            val saveDir = File(context.cacheDir, "torrents")
            if (!saveDir.exists()) saveDir.mkdirs()

            // Use reflection to call AddTorrentParams.parseMagnetUri(magnetUri)
            val atpClass = Class.forName("com.frostwire.jlibtorrent.AddTorrentParams")
            val parseMethod = atpClass.getMethod("parseMagnetUri", String::class.java)
            val addTorrentParams = parseMethod.invoke(null, magnetUri)

            // set save path
            val savePathField = atpClass.getField("savePath")
            savePathField.set(addTorrentParams, saveDir.absolutePath)

            val clazz = sessionManager!!::class.java
            val addMethod = clazz.getMethod("addTorrent", atpClass)
            val torrentHandle = addMethod.invoke(sessionManager, addTorrentParams)

            // wait for metadata
            val thClass = torrentHandle::class.java
            var attempts = 0
            while (true) {
                val hasMetadataMethod = thClass.getMethod("hasMetadata")
                val has = hasMetadataMethod.invoke(torrentHandle) as Boolean
                if (has) break
                delay(500)
                attempts++
                if (attempts > 120) {
                    Log.e(TAG, "timeout waiting metadata")
                    return@withContext null
                }
            }

            // get torrent info and choose largest file
            val torrentInfoMethod = thClass.getMethod("torrentFile")
            val ti = torrentInfoMethod.invoke(torrentHandle)
            val tiClass = ti::class.java
            val filesMethod = tiClass.getMethod("files")
            val files = filesMethod.invoke(ti)
            val filesClass = files::class.java
            val numFilesMethod = filesClass.getMethod("numFiles")
            val numFiles = numFilesMethod.invoke(files) as Int
            var largestIndex = 0
            var largestSize = 0L
            val fileSizeMethod = filesClass.getMethod("fileSize", Int::class.javaPrimitiveType)
            val fileNameMethod = filesClass.getMethod("fileName", Int::class.javaPrimitiveType)
            for (i in 0 until numFiles) {
                val sz = fileSizeMethod.invoke(files, i) as Long
                if (sz > largestSize) {
                    largestSize = sz
                    largestIndex = i
                }
            }

            val chosenName = fileNameMethod.invoke(files, largestIndex) as String

            // Start a simple local HTTP server to serve the saveDir
            val server = LocalHttpServer(0, saveDir)
            server.start()
            val port = server.listeningPort

            val encoded = URLEncoder.encode(chosenName, "UTF-8")
            val url = "http://127.0.0.1:$port/$encoded"

            Log.i(TAG, "local url: $url")

            return@withContext url
        } catch (e: Exception) {
            Log.e(TAG, "error starting torrent", e)
            return@withContext null
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        try {
            if (sessionManager != null) {
                val clazz = sessionManager!!::class.java
                val stopMethod = clazz.getMethod("stop")
                stopMethod.invoke(sessionManager)
                sessionManager = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "error stopping session", e)
        }
    }
}
