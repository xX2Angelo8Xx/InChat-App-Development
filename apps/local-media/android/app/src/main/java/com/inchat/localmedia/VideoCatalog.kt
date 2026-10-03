package com.inchat.localmedia

import org.json.JSONObject

// Only retain metadata needed by the preview and explicit single-track selectors.
data class MediaFormat(val id: String, val extension: String, val codec: String,
    val height: Int = 0, val fps: Int = 0, val bitrate: Int = 0, val hasAudio: Boolean = false) {
    val label: String get() = if (height > 0) "${height}p${if (fps > 30) " · $fps fps" else ""}"
        else "${if (bitrate > 0) "ca. $bitrate kbit/s · " else ""}${if (codec.startsWith("mp4a")) "AAC" else codec.uppercase()}"
}
data class VideoDetails(val url: String, val id: String, val title: String, val duration: Int,
    val thumbnail: String, val audio: List<MediaFormat>, val video: List<MediaFormat>)

object VideoCatalog {
    fun parse(url: String, json: String): VideoDetails {
        val data = JSONObject(json)
        require(data.optString("_type", "video") !in listOf("playlist", "multi_video")) { "Bitte ein einzelnes Video wählen." }
        val id = data.optString("id")
        require(Regex("[A-Za-z0-9_-]{11}").matches(id)) { "Keine gültigen Videoinformationen erhalten." }
        val title = data.optString("title").take(500)
        require(title.isNotBlank()) { "Kein Videotitel erhalten." }
        val formats = data.optJSONArray("formats") ?: throw IllegalArgumentException("Keine verfügbaren Formate erhalten.")
        val audio = mutableListOf<MediaFormat>()
        val video = mutableListOf<MediaFormat>()
        for (i in 0 until formats.length()) {
            val f = formats.optJSONObject(i) ?: continue
            val formatId = f.optString("format_id")
            if (!Regex("[A-Za-z0-9_.-]{1,100}").matches(formatId) || f.optBoolean("has_drm", false)) continue
            if (!f.optString("url").startsWith("https://")) continue
            val vcodec = f.optString("vcodec", "none")
            val acodec = f.optString("acodec", "none")
            val ext = f.optString("ext")
            val abr = f.optDouble("abr", f.optDouble("tbr", 0.0)).takeIf { it.isFinite() }?.toInt() ?: 0
            if (vcodec == "none" && acodec !in listOf("none", "", "null"))
                audio += MediaFormat(formatId, ext, acodec, bitrate = abr.coerceIn(0, 10000), hasAudio = true)
            val height = f.optInt("height", 0)
            if (ext == "mp4" && (vcodec.startsWith("avc1") || vcodec.startsWith("h264")) && height in 1..4320)
                video += MediaFormat(formatId, ext, vcodec, height, f.optDouble("fps", 0.0).toInt().coerceIn(0, 240),
                    hasAudio = acodec !in listOf("none", "", "null"))
        }
        val audioChoices = audio.sortedByDescending { it.bitrate }.distinctBy { Pair(it.codec, it.bitrate) }
        val videoChoices = video.filter { it.hasAudio || audioChoices.isNotEmpty() }
            .sortedWith(compareBy<MediaFormat> { it.height }.thenBy { it.fps }.thenByDescending { it.hasAudio })
            .distinctBy { Pair(it.height, it.fps) }
        require(audioChoices.isNotEmpty() || videoChoices.isNotEmpty()) { "Keine unterstützten Audio- oder MP4-Formate verfügbar." }
        return VideoDetails(url, id, title, data.optDouble("duration", 0.0).toInt().coerceAtLeast(0),
            data.optString("thumbnail"), audioChoices, videoChoices)
    }
}
