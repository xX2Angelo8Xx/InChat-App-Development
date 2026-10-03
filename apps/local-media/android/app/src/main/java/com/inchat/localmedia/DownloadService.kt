package com.inchat.localmedia

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class DownloadService : Service() {
    private val executor = Executors.newSingleThreadExecutor()
    private val cancelled = AtomicBoolean(false)
    private val active = AtomicBoolean(false)
    private var wake: PowerManager.WakeLock? = null
    private var processId = ""
    private var lastNotification = 0L
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("downloads", "Downloads", NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "cancel") { cancel(); return START_NOT_STICKY }
        if (!active.compareAndSet(false, true)) return START_NOT_STICKY
        if (!DownloadState.current.busy) DownloadState.begin()
        startForeground(1, notification("Download wird vorbereitet …"))
        cancelled.set(false)
        processId = "download-$startId"
        val url = intent?.getStringExtra("url").orEmpty()
        val mp3 = intent?.getBooleanExtra("mp3", true) ?: true
        val quality = intent?.getIntExtra("quality", 720) ?: 720
        wake = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocalMedia:Download").also { it.acquire(2 * 60 * 60 * 1000L) }
        executor.execute { download(url, mp3, quality) }
        return START_NOT_STICKY
    }
    private fun download(input: String, mp3: Boolean, quality: Int) {
        val directory = File(cacheDir, "download-job")
        try {
            val url = DownloadOptions.normalize(input)
            directory.deleteRecursively(); check(directory.mkdirs())
            MediaFiles.recover(this)
            update("Lokale Komponenten werden geladen …")
            YoutubeDL.init(this); FFmpeg.init(this)
            checkNotCancelled()
            val request = YoutubeDLRequest(url).addCommands(DownloadOptions.arguments(mp3, quality, directory.absolutePath))
            update("Video wird abgerufen …")
            YoutubeDL.execute(request, processId, false) { progress, eta, line ->
                if (cancelled.get()) YoutubeDL.destroyProcessById(processId)
                else {
                    val converting = line.contains("[ExtractAudio]") || line.contains("[Merger]") || line.contains("[VideoRemuxer]")
                    val percent = progress.toInt().coerceIn(0, 100)
                    val message = if (converting) "${if (mp3) "MP3 wird erstellt" else "Video und Audio werden zusammengeführt"} …"
                        else "Download: $percent %${if (eta > 0) " · ca. $eta s" else ""}"
                    update(message, if (converting) -1 else percent)
                }
            }
            checkNotCancelled()
            val extension = if (mp3) "mp3" else "mp4"
            val files = directory.listFiles()?.filter { it.extension == extension && it.length() > 0 }.orEmpty()
            check(files.size == 1) { "Keine vollständige $extension-Datei erzeugt." }
            update("Datei wird im Download-Ordner gespeichert …")
            val file = MediaFiles.publish(this, files.single())
            DownloadState.publish(JobState(message = "Gespeichert: ${file.name}"))
        } catch (e: Exception) {
            val state = if (cancelled.get()) JobState(message = "Download abgebrochen.")
                else JobState(message = "Download fehlgeschlagen.", error = e.message.orEmpty().takeLast(5000))
            DownloadState.publish(state)
        } finally {
            directory.deleteRecursively()
            wake?.let { if (it.isHeld) it.release() }
            active.set(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
    private fun checkNotCancelled() { check(!cancelled.get()) { "Abgebrochen" } }
    private fun update(message: String, progress: Int = -1) {
        DownloadState.publish(JobState(true, message, progress))
        val now = System.currentTimeMillis()
        if (now - lastNotification > 750) {
            lastNotification = now
            if (android.os.Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED)
                getSystemService(NotificationManager::class.java).notify(1, notification(message, progress))
        }
    }
    private fun notification(message: String, progress: Int = -1): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val cancel = PendingIntent.getService(this, 1, Intent(this, DownloadService::class.java).setAction("cancel"), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, "downloads").setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Local Media").setContentText(message).setContentIntent(open).setOngoing(true)
            .setProgress(100, progress.coerceAtLeast(0), progress < 0)
            .addAction(Notification.Action.Builder(null, "Abbrechen", cancel).build()).build()
    }
    private fun cancel() {
        cancelled.set(true)
        // Cancellation may arrive while native packages are still being initialized.
        if (processId.isNotEmpty()) YoutubeDL.destroyProcessById(processId)
    }
    override fun onTimeout(startId: Int, fgsType: Int) { cancel(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    override fun onDestroy() { cancel(); executor.shutdown(); super.onDestroy() }
}
