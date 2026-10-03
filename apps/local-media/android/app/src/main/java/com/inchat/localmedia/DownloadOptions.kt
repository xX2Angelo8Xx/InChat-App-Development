package com.inchat.localmedia

import java.net.URI

object DownloadOptions {
    fun normalize(input: String): String {
        val text = input.trim()
        val uri = URI(text)
        require(uri.scheme == "https" && uri.userInfo == null && (uri.port == -1 || uri.port == 443)) {
            "Bitte einen vollständigen HTTPS-Link von YouTube einfügen."
        }
        val host = uri.host?.lowercase() ?: ""
        require(host in setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be")) {
            "Dieser Link gehört nicht zu YouTube."
        }
        val id = if (host == "youtu.be") uri.path.removePrefix("/").substringBefore('/') else {
            when {
                uri.path == "/watch" -> uri.rawQuery.orEmpty().split('&').firstOrNull { it.startsWith("v=") }?.removePrefix("v=").orEmpty()
                uri.path.startsWith("/shorts/") || uri.path.startsWith("/live/") -> uri.path.split('/').getOrNull(2).orEmpty()
                else -> ""
            }
        }
        require(Regex("[A-Za-z0-9_-]{11}").matches(id)) { "Bitte einen Link zu einem einzelnen Video verwenden."
        }
        return "https://www.youtube.com/watch?v=$id"
    }

    fun arguments(mp3: Boolean, quality: Int, directory: String): List<String> {
        require(quality in setOf(360, 720, 1080))
        val options = mutableListOf("--ignore-config", "--no-playlist", "--no-mtime", "--newline",
            "--socket-timeout", "30", "--retries", "3", "--fragment-retries", "3",
            "--restrict-filenames", "--trim-filenames", "140", "-o", "$directory/%(title).120B [%(id)s].%(ext)s")
        if (mp3) options += listOf("-f", "bestaudio/best", "-x", "--audio-format", "mp3", "--audio-quality", "192K")
        else options += listOf("-f", "bestvideo[ext=mp4][vcodec^=avc1][height<=$quality]+bestaudio[ext=m4a]/best[ext=mp4][height<=$quality]",
            "--merge-output-format", "mp4", "--remux-video", "mp4")
        return options
    }
}
