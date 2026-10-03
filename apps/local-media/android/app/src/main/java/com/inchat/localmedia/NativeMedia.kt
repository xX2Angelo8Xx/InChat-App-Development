package com.inchat.localmedia

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** Owns conversion explicitly: inputs and output are different files; source tracks stay intact. */
class NativeMedia(private val context: Context, private val cancelled: AtomicBoolean) {
    @Volatile private var process: Process? = null
    fun cancel() { process?.destroy() }
    fun convert(source: File, audio: File?, target: File, mp3: Boolean) {
        check(!cancelled.get()) { "Abgebrochen" }
        require(source.isFile && source.length() > 0) { "Die heruntergeladene Quelldatei fehlt." }
        audio?.let { require(it.isFile && it.length() > 0) { "Die Audiospur fehlt." } }
        require(source.canonicalPath != target.canonicalPath)
        target.parentFile?.mkdirs()
        YoutubeDL.init(context); FFmpeg.init(context)
        val args = mutableListOf("${context.applicationInfo.nativeLibraryDir}/libffmpeg.so", "-y", "-hide_banner", "-nostdin", "-i", source.absolutePath)
        if (audio != null) args += listOf("-i", audio.absolutePath)
        if (mp3) args += listOf("-map", "0:a:0", "-vn", "-c:a", "libmp3lame", "-b:a", "192k")
        else args += listOf("-map", "0:v:0", "-map", if (audio == null) "0:a:0" else "1:a:0", "-c:v", "copy", "-c:a", "aac", "-b:a", "192k", "-movflags", "+faststart")
        args += target.absolutePath
        val builder = ProcessBuilder(args).redirectErrorStream(true)
        builder.environment()["LD_LIBRARY_PATH"] = listOf("python", "ffmpeg", "aria2c").joinToString(":") {
            File(context.noBackupFilesDir, "youtubedl-android/packages/$it/usr/lib").absolutePath
        }
        val log = StringBuilder()
        try {
            val child = builder.start(); process = child
            if (cancelled.get()) child.destroy()
            child.inputStream.bufferedReader().useLines { lines -> lines.forEach {
                log.append(it).append('\n'); if (log.length > 6000) log.delete(0, log.length - 6000)
            } }
            val exit = child.waitFor()
            check(!cancelled.get()) { "Abgebrochen" }
            check(exit == 0) { "FFmpeg-Konvertierung fehlgeschlagen (Code $exit):\n$log" }
            MediaValidation.verify(target)
        } finally { process?.destroy(); process = null }
    }
}
