package com.inchat.localmedia

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors

// Native initialization and subprocesses are serialized across search and download workers.
object EngineSession { val lock = Any() }
data class SearchState(val busy: Boolean = false, val query: String = "", val details: VideoDetails? = null,
    val thumbnail: Bitmap? = null, val error: String = "")

object VideoSearch {
    @Volatile var current = SearchState()
        private set
    private val main = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<(SearchState) -> Unit>()
    private val executor = Executors.newSingleThreadExecutor()
    private var generation = 0L
    private var processId = ""
    @Synchronized fun start(context: Context, url: String) {
        if (current.busy || DownloadState.current.busy) return
        val token = ++generation
        val id = "search-${UUID.randomUUID()}"
        processId = id
        publish(SearchState(busy = true, query = url))
        val app = context.applicationContext
        executor.execute {
            try {
                val details = synchronized(EngineSession.lock) {
                    if (!isCurrent(token)) return@execute
                    YoutubeDL.init(app); com.yausername.ffmpeg.FFmpeg.init(app)
                    if (!isCurrent(token)) return@execute
                    val request = YoutubeDLRequest(url).addCommands(listOf("--ignore-config", "--no-playlist",
                        "--skip-download", "--dump-single-json", "--socket-timeout", "20", "--retries", "2"))
                    VideoCatalog.parse(url, YoutubeDL.execute(request, id, false).out)
                }
                complete(token, SearchState(query = url, details = details))
                val bitmap = loadThumbnail(details.thumbnail)
                if (bitmap != null) complete(token, SearchState(query = url, details = details, thumbnail = bitmap))
            } catch (e: Exception) {
                complete(token, SearchState(query = url, error = e.message.orEmpty().takeLast(6000)))
            }
        }
    }
    @Synchronized private fun isCurrent(token: Long) = token == generation
    @Synchronized private fun complete(token: Long, state: SearchState) { if (token == generation) publish(state) }
    @Synchronized fun clear() {
        ++generation
        if (current.busy && processId.isNotBlank()) YoutubeDL.destroyProcessById(processId)
        publish(SearchState())
    }
    internal fun publish(state: SearchState) {
        current = state
        main.post { listeners.forEach { it(current) } }
    }
    fun observe(listener: (SearchState) -> Unit) { listeners.add(listener); listener(current) }
    fun remove(listener: (SearchState) -> Unit) { listeners.remove(listener) }

    private fun loadThumbnail(value: String): Bitmap? = try {
        var url = URI(value)
        var image: Bitmap? = null
        for (redirect in 0..3) {
            val host = url.host?.lowercase().orEmpty()
            require(url.scheme == "https" && url.userInfo == null && (url.port == -1 || url.port == 443)
                && (host == "ytimg.com" || host.endsWith(".ytimg.com") || host == "img.youtube.com"))
            val connection = url.toURL().openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10000; connection.readTimeout = 10000; connection.instanceFollowRedirects = false
                if (connection.responseCode in 300..399) {
                    url = url.resolve(requireNotNull(connection.getHeaderField("Location")))
                    continue
                }
                require(connection.responseCode == 200)
                val bytes = connection.inputStream.use { it.readBytesBounded(2 * 1024 * 1024) }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                require(bounds.outWidth in 1..8192 && bounds.outHeight in 1..8192)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 1
                    while (bounds.outWidth / inSampleSize > 960 || bounds.outHeight / inSampleSize > 540) inSampleSize *= 2
                }
                image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                break
            } finally { connection.disconnect() }
        }
        image
    } catch (_: Exception) { null }
    private fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer); if (count < 0) break
            require(output.size() + count <= limit)
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
