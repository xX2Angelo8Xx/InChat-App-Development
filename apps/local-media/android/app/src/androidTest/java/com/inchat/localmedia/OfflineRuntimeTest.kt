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
    @Test fun previewPrecedesFormatSelectionAndChangingLinkInvalidatesIt() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = android.content.Intent(instrumentation.targetContext, MainActivity::class.java)
            .setAction(android.content.Intent.ACTION_SEND).putExtra(android.content.Intent.EXTRA_TEXT, "https://youtu.be/BaW_jenozKc")
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        VideoSearch.clear()
        val activity = instrumentation.startActivitySync(intent) as MainActivity
        instrumentation.waitForIdleSync()
        fun descendants(view: android.view.View): List<android.view.View> = if (view is android.view.ViewGroup)
            listOf(view) + (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else listOf(view)
        val bitmap = android.graphics.Bitmap.createBitmap(640, 360, android.graphics.Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(bitmap).apply {
            drawColor(android.graphics.Color.rgb(25, 65, 55))
            drawText("VIDEO PREVIEW", 45f, 185f, android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE; textSize = 48f; isAntiAlias = true
            })
        }
        try {
            instrumentation.runOnMainSync {
                val views = descendants(activity.window.decorView)
                assertEquals("https://youtu.be/BaW_jenozKc", views.filterIsInstance<android.widget.EditText>().single().text.toString())
                assertFalse(DownloadState.current.busy); assertFalse(VideoSearch.current.busy)
                assertFalse(views.filterIsInstance<android.widget.Button>().first { it.text == "Herunterladen" }.isEnabled)
                assertFalse(views.filterIsInstance<android.widget.RadioButton>().first { it.text.toString().startsWith("MP4") }.isShown)
            }
            val fixture = instrumentation.context.assets.open("catalog-fixture.json").bufferedReader().use { it.readText() }
            val url = DownloadOptions.normalize("https://youtu.be/BaW_jenozKc")
            val video = VideoCatalog.parse(url, fixture)
            VideoSearch.publish(SearchState(query = url, details = video, thumbnail = bitmap))
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                val views = descendants(activity.window.decorView)
                assertTrue(views.filterIsInstance<android.widget.TextView>().any { it.text == video.title && it.isShown })
                assertNotNull(views.filterIsInstance<android.widget.ImageView>().single().drawable)
                views.filterIsInstance<android.widget.RadioButton>().first { it.text.toString().startsWith("MP4") }.performClick()
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                val views = descendants(activity.window.decorView)
                val spinner = views.filterIsInstance<android.widget.Spinner>().single()
                assertEquals(listOf("720p", "1080p · 60 fps"), (0 until spinner.count).map { spinner.getItemAtPosition(it) })
                spinner.setSelection(1)
                assertTrue(views.filterIsInstance<android.widget.Button>().first { it.text == "Herunterladen" }.isEnabled)
            }
            instrumentation.waitForIdleSync()
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            assertNotNull(screenshot)
            File(activity.getExternalFilesDir(null), "ui-preview.png").outputStream().use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            screenshot.recycle()
            // UTP uninstalls the test app afterward; export while its external folder exists.
            val export = instrumentation.uiAutomation.executeShellCommand(
                "cp /sdcard/Android/data/com.inchat.localmedia/files/ui-preview.png /data/local/tmp/local-media-preview.png 2>&1")
            val exportError = android.os.ParcelFileDescriptor.AutoCloseInputStream(export).bufferedReader().use { it.readText() }
            assertEquals("Could not export the populated preview", "", exportError)
            instrumentation.runOnMainSync {
                val views = descendants(activity.window.decorView)
                views.filterIsInstance<android.widget.EditText>().single().setText("https://youtu.be/OETnuwwsv9U")
                assertFalse(views.filterIsInstance<android.widget.Button>().first { it.text == "Herunterladen" }.isEnabled)
                assertFalse(views.filterIsInstance<android.widget.ImageView>().single().isShown)
            }
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
            VideoSearch.clear()
            bitmap.recycle()
        }
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
