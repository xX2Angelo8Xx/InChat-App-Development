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

    fun trackArguments(formatId: String, directory: String): List<String> {
        require(Regex("[A-Za-z0-9_.-]{1,100}").matches(formatId)) { "Ungültige Formatauswahl. Bitte erneut suchen." }
        return listOf("--ignore-config", "--no-playlist", "--no-mtime", "--newline", "--fixup", "never",
            "--socket-timeout", "30", "--retries", "3", "--fragment-retries", "3",
            "-f", formatId, "-o", "$directory/source.%(ext)s")
    }

    fun safeTitle(title: String): String {
        val cleaned = title.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().trim('.')
        val output = StringBuilder()
        var bytes = 0
        cleaned.codePoints().toArray().forEach { cp ->
            val character = String(Character.toChars(cp))
            if (bytes + character.toByteArray(Charsets.UTF_8).size <= 160) {
                output.append(character); bytes += character.toByteArray(Charsets.UTF_8).size
            }
        }
        return output.toString().ifBlank { "Video" }
    }
}
