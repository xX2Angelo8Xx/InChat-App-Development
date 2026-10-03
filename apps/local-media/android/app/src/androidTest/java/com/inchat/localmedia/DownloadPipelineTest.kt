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
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class DownloadPipelineTest {
    @Test fun actualWebmDownloadConvertsToMp3WithoutRenamingOrDeletingTheSource() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        synchronized(EngineSession.lock) {
            YoutubeDL.init(context); FFmpeg.init(context)
            val work = File(context.noBackupFilesDir, "pipeline-test-${UUID.randomUUID()}").apply { mkdirs() }
            try {
                val fixture = File(work, "fixture.webm")
                val builder = ProcessBuilder("${context.applicationInfo.nativeLibraryDir}/libffmpeg.so", "-y", "-hide_banner",
                    "-f", "lavfi", "-i", "sine=frequency=440:duration=0.5", "-c:a", "libopus", fixture.absolutePath).redirectErrorStream(true)
                builder.environment()["LD_LIBRARY_PATH"] = listOf("python", "ffmpeg", "aria2c").joinToString(":") {
                    File(context.noBackupFilesDir, "youtubedl-android/packages/$it/usr/lib").absolutePath
                }
                val process = builder.start(); val log = process.inputStream.bufferedReader().readText()
                assertEquals(log, 0, process.waitFor())
                val track = File(work, "track").apply { mkdirs() }
                val request = YoutubeDLRequest(fixture.toURI().toString()).addOption("--enable-file-urls")
                    .addCommands(DownloadOptions.trackArguments("best", track.absolutePath))
                YoutubeDL.execute(request, "pipeline-test-${UUID.randomUUID()}", false)
                val downloaded = requireNotNull(track.listFiles()).single { it.name == "source.webm" }
                assertTrue(downloaded.length() > 100)
                val target = File(work, "Regression [BaW_jenozKc].mp3")
                NativeMedia(context, AtomicBoolean(false)).convert(downloaded, null, target, true)
                assertTrue("Conversion must preserve its source until the job is committed", downloaded.isFile)
                MediaValidation.verify(target)
                assertTrue(target.length() > 100)
                val saved = MediaFiles.publish(context, target)
                assertNotNull(context.contentResolver.openInputStream(android.net.Uri.parse(saved.uri))?.use { assertTrue(it.read() >= 0) })
                context.contentResolver.delete(android.net.Uri.parse(saved.uri), null, null)
                context.getSharedPreferences("history", 0).edit().clear().commit()
                val video = File(work, "picture.mp4")
                val videoProcess = ProcessBuilder("${context.applicationInfo.nativeLibraryDir}/libffmpeg.so", "-y", "-hide_banner",
                    "-f", "lavfi", "-i", "color=size=64x64:rate=10:duration=0.5", "-c:v", "libx264", "-an", video.absolutePath)
                    .redirectErrorStream(true).apply { environment()["LD_LIBRARY_PATH"] = builder.environment()["LD_LIBRARY_PATH"] }.start()
                val videoLog = videoProcess.inputStream.bufferedReader().readText()
                assertEquals(videoLog, 0, videoProcess.waitFor())
                val mp4 = File(work, "Regression.mp4")
                NativeMedia(context, AtomicBoolean(false)).convert(video, downloaded, mp4, false)
                MediaValidation.verify(mp4); assertTrue(video.isFile); assertTrue(downloaded.isFile)
            } finally { work.deleteRecursively() }
        }
    }
}
