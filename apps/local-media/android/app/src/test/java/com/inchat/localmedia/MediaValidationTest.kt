package com.inchat.localmedia

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class MediaValidationTest {
    @Test fun rejectsHtmlErrorPagesRegardlessOfFilename() {
        listOf("mp3", "mp4").forEach { extension ->
            val file = File.createTempFile("network-error", ".$extension")
            try {
                file.writeText("<html><body>Site Unavailable</body></html>")
                try { MediaValidation.verify(file); fail("HTML must not be published as media") }
                catch (_: IllegalStateException) { }
            } finally { file.delete() }
        }
    }
    @Test fun rejectsIncompleteFiles() {
        val file = File.createTempFile("incomplete", ".mp4")
        try {
            file.writeBytes(byteArrayOf(0, 0, 0))
            try { MediaValidation.verify(file); fail("Incomplete output must not be published") }
            catch (_: IllegalStateException) { }
        } finally { file.delete() }
    }
}
