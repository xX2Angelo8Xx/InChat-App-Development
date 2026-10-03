package com.inchat.localmedia

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class OfflineRuntimeTest {
    @Test fun interfaceAcceptsSharedLinkWithoutAutomaticDownload() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = android.content.Intent(instrumentation.targetContext, MainActivity::class.java)
            .setAction(android.content.Intent.ACTION_SEND).putExtra(android.content.Intent.EXTRA_TEXT, "https://youtu.be/BaW_jenozKc")
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = instrumentation.startActivitySync(intent) as MainActivity
        instrumentation.waitForIdleSync()
        fun descendants(view: android.view.View): List<android.view.View> = if (view is android.view.ViewGroup)
            listOf(view) + (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else listOf(view)
        try {
            instrumentation.runOnMainSync {
                val views = descendants(activity.window.decorView)
                assertEquals("https://youtu.be/BaW_jenozKc", views.filterIsInstance<android.widget.EditText>().single().text.toString())
                assertFalse(DownloadState.current.busy)
                val radio = views.filterIsInstance<android.widget.RadioButton>().first { it.text.toString().startsWith("MP4") }
                radio.performClick()
                assertEquals(android.view.View.VISIBLE, views.filterIsInstance<android.widget.Spinner>().single().visibility)
            }
            instrumentation.waitForIdleSync()
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            assertNotNull(screenshot)
            File(activity.getExternalFilesDir(null), "ui-preview.png").outputStream().use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            screenshot.recycle()
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }

    @Test fun packagedRuntimeCreatesRealMp3AndMp4WithoutNetwork() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        YoutubeDL.init(context); FFmpeg.init(context)
        assertTrue(YoutubeDL.execute(YoutubeDLRequest(emptyList()).addOption("--version")).out.contains("2026.08.19"))
        val native = context.applicationInfo.nativeLibraryDir
        val qjs = ProcessBuilder("$native/libqjs.so", "--eval", "console.log(6*7)").redirectErrorStream(true).start()
        val jsOutput = qjs.inputStream.bufferedReader().readText()
        assertTrue(qjs.waitFor(20, TimeUnit.SECONDS)); assertEquals(jsOutput, 0, qjs.exitValue()); assertTrue(jsOutput.contains("42"))
        val output = File(context.cacheDir, "runtime-check").apply { mkdirs() }
        fun ffmpeg(arguments: List<String>) {
            val pb = ProcessBuilder(listOf("$native/libffmpeg.so", "-y", "-hide_banner") + arguments).redirectErrorStream(true)
            pb.environment()["LD_LIBRARY_PATH"] = listOf("python", "ffmpeg", "aria2c").joinToString(":") { File(context.noBackupFilesDir, "youtubedl-android/packages/$it/usr/lib").absolutePath }
            val process = pb.start()
            val log = process.inputStream.bufferedReader().readText()
            assertTrue(process.waitFor(60, TimeUnit.SECONDS)); assertEquals(log, 0, process.exitValue())
        }
        try {
            val mp3 = File(output, "test.mp3")
            ffmpeg(listOf("-f", "lavfi", "-i", "sine=frequency=440:duration=0.3", "-c:a", "libmp3lame", mp3.absolutePath))
            assertTrue(mp3.length() > 500)
            val mp4 = File(output, "test.mp4")
            ffmpeg(listOf("-f", "lavfi", "-i", "color=size=64x64:rate=10", "-f", "lavfi", "-i", "sine=frequency=440", "-t", "0.3", "-c:v", "mpeg4", "-c:a", "aac", mp4.absolutePath))
            assertTrue(mp4.length() > 500)
            MediaValidation.verify(mp4)
            val saved = MediaFiles.publish(context, mp3)
            assertNotNull(context.contentResolver.openInputStream(android.net.Uri.parse(saved.uri))?.use { assertTrue(it.read() >= 0) })
            context.contentResolver.delete(android.net.Uri.parse(saved.uri), null, null)
            context.getSharedPreferences("history", 0).edit().clear().commit()
        } finally { output.deleteRecursively() }
    }
}
