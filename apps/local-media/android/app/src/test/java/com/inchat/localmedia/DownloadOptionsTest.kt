package com.inchat.localmedia
import org.junit.Assert.*
import org.junit.Test

class DownloadOptionsTest {
    @Test fun supportedLinksBecomeSingleVideo() {
        listOf("https://youtu.be/BaW_jenozKc?t=4", "https://www.youtube.com/watch?v=BaW_jenozKc&list=PL1", "https://m.youtube.com/shorts/BaW_jenozKc", "https://youtube.com/live/BaW_jenozKc").forEach {
            assertEquals("https://www.youtube.com/watch?v=BaW_jenozKc", DownloadOptions.normalize(it))
        }
    }
    @Test fun rejectsUntrustedOrUnsupportedUrls() {
        listOf("http://youtu.be/BaW_jenozKc", "https://youtube.com.evil.org/watch?v=BaW_jenozKc", "https://evil.org/", "https://www.youtube.com/playlist?list=x", "https://www.youtube.com/watch?v=bad", "https://you@youtube.com/watch?v=BaW_jenozKc", "https://youtube.com:444/watch?v=BaW_jenozKc").forEach {
            try { DownloadOptions.normalize(it); fail(it) } catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun mp3IsConvertedAndVideoIncludesAudio() {
        val audio = DownloadOptions.arguments(true, 720, "/tmp/media")
        assertTrue(audio.contains("--audio-format")); assertTrue(audio.contains("mp3"))
        val video = DownloadOptions.arguments(false, 1080, "/tmp/media")
        assertTrue(video.contains("bestvideo[ext=mp4][vcodec^=avc1][height<=1080]+bestaudio[ext=m4a]/best[ext=mp4][height<=1080]"))
        assertTrue(video.contains("--merge-output-format")); assertTrue(video.contains("--no-playlist"))
    }
}
