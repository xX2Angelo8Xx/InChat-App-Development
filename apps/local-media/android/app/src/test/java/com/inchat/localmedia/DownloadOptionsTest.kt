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
    @Test fun individualTracksDoNotRunTheOldAudioPostprocessor() {
        val args = DownloadOptions.trackArguments("251", "/tmp/media")
        assertTrue(args.contains("251")); assertTrue(args.contains("--no-playlist"))
        assertTrue(args.contains("never")); assertFalse(args.contains("-x"))
        assertFalse(args.contains("--audio-format"))
        try { DownloadOptions.trackArguments("251;rm", "/tmp/media"); fail() } catch (_: IllegalArgumentException) { }
    }
    @Test fun outputNamesAreBoundedAndCannotContainDirectories() {
        val title = DownloadOptions.safeTitle("../A/B: test?" + "🚀".repeat(100))
        assertFalse(title.contains('/')); assertFalse(title.contains(':'))
        assertTrue(title.toByteArray().size <= 160)
    }
}
