package com.inchat.localmedia

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArraySet

data class JobState(val busy: Boolean = false, val message: String = "Bereit für deinen Link.", val progress: Int = -1, val error: String = "")

object DownloadState {
    @Volatile var current = JobState()
        private set
    private val listeners = CopyOnWriteArraySet<(JobState) -> Unit>()
    private val main = Handler(Looper.getMainLooper())
    @Synchronized fun begin(): Boolean {
        if (current.busy) return false
        publish(JobState(true, "Download wird vorbereitet …"))
        return true
    }
    fun publish(state: JobState) {
        current = state
        main.post { listeners.forEach { it(current) } }
    }
    fun observe(listener: (JobState) -> Unit) { listeners.add(listener); listener(current) }
    fun remove(listener: (JobState) -> Unit) { listeners.remove(listener) }
}
