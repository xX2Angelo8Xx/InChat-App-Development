package com.inchat.localmedia

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.*

class MainActivity : Activity() {
    private lateinit var url: EditText
    private lateinit var mp3: RadioButton
    private lateinit var mp4: RadioButton
    private lateinit var quality: Spinner
    private lateinit var start: Button
    private lateinit var cancel: Button
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var details: Button
    private lateinit var history: LinearLayout
    private var lastError = ""
    private val observer: (JobState) -> Unit = { render(it) }
    private val green = Color.rgb(23, 107, 89)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 247, 244))
        }
        root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(26), dp(24), dp(24)) }
        root.addView(scroll); scroll.addView(column); setContentView(root); root.requestApplyInsets()
        fun label(text: String, size: Float = 16f, bold: Boolean = false): TextView = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(Color.rgb(25, 42, 37)); setPadding(0, dp(8), 0, dp(8))
            if (bold) setTypeface(null, Typeface.BOLD)
        }.also { column.addView(it) }
        label("LOCAL MEDIA", 12f, true).setTextColor(green)
        label("Dein Link.\nDeine Datei.", 32f, true)
        label("MP3 oder MP4 · direkt auf deinem Gerät", 15f)
        label("YouTube-Link", 14f, true)
        url = EditText(this).apply {
            hint = "https://youtu.be/…"; textSize = 16f; setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(12).toFloat(); setStroke(dp(1), Color.rgb(209, 222, 214)) }
        }; column.addView(url, LinearLayout.LayoutParams(-1, dp(56)))
        Button(this).apply {
            text = "Link einfügen"
            setOnClickListener {
                val clip = (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
                val text = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(this@MainActivity).toString() else ""
                if (text.isNotBlank()) url.setText(extractLink(text)) else Toast.makeText(this@MainActivity, "Zwischenablage ist leer.", Toast.LENGTH_SHORT).show()
            }
        }.also { column.addView(it) }
        label("Format", 14f, true)
        val format = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        mp3 = RadioButton(this).apply { id = View.generateViewId(); text = "MP3 · Audio"; isChecked = true }
        mp4 = RadioButton(this).apply { id = View.generateViewId(); text = "MP4 · Video" }
        format.addView(mp3, RadioGroup.LayoutParams(0, dp(52), 1f)); format.addView(mp4, RadioGroup.LayoutParams(0, dp(52), 1f)); column.addView(format)
        quality = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Video: bis 360p", "Video: bis 720p", "Video: bis 1080p"))
            setSelection(1); visibility = View.GONE
        }; column.addView(quality)
        val hint = label("MP3: 192 kbit/s. Die Quellqualität bleibt maßgeblich.", 13f)
        format.setOnCheckedChangeListener { _, _ ->
            quality.visibility = if (mp4.isChecked) View.VISIBLE else View.GONE
            hint.text = if (mp3.isChecked) "MP3: 192 kbit/s. Die Quellqualität bleibt maßgeblich." else "MP4 mit Bild und Ton. Gewählte Auflösung ist die Obergrenze."
        }
        start = Button(this).apply {
            text = "Herunterladen"; setTextColor(Color.WHITE); backgroundTintList = android.content.res.ColorStateList.valueOf(green)
            setOnClickListener { startDownload() }
        }; column.addView(start, LinearLayout.LayoutParams(-1, dp(58)))
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; visibility = View.GONE }
        column.addView(progress)
        status = label("Bereit für deinen Link.", 14f)
        details = Button(this).apply {
            text = "Fehlerdetails anzeigen"; visibility = View.GONE
            setOnClickListener { AlertDialog.Builder(this@MainActivity).setTitle("Download-Fehler").setMessage(lastError).setPositiveButton("Schließen", null).show() }
        }; column.addView(details)
        cancel = Button(this).apply {
            text = "Abbrechen"; visibility = View.GONE
            setOnClickListener { startService(Intent(this@MainActivity, DownloadService::class.java).setAction("cancel")); isEnabled = false }
        }; column.addView(cancel)
        label("Letzte Downloads", 19f, true)
        history = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; column.addView(history)
        label("Gespeichert unter Downloads / Local Media.\nVerarbeitung lokal · keine Konten · kein eigener Server.", 12f)
        receive(intent)
        if (savedInstanceState != null) {
            url.setText(savedInstanceState.getString("url", ""))
            if (!savedInstanceState.getBoolean("mp3", true)) mp4.isChecked = true
            quality.setSelection(savedInstanceState.getInt("quality", 1))
        }
    }
    private fun extractLink(text: String) = Regex("https://[^\\s<>]+").find(text)?.value?.trimEnd('.', ',', ')') ?: text.trim()
    private fun receive(value: Intent) { if (value.action == Intent.ACTION_SEND) url.setText(extractLink(value.getStringExtra(Intent.EXTRA_TEXT).orEmpty())) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); receive(intent) }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("url", url.text.toString()); outState.putBoolean("mp3", mp3.isChecked); outState.putInt("quality", quality.selectedItemPosition)
        super.onSaveInstanceState(outState)
    }
    override fun onStart() { super.onStart(); DownloadState.observe(observer) }
    override fun onStop() { DownloadState.remove(observer); super.onStop() }
    private fun startDownload() {
        val input = try { DownloadOptions.normalize(url.text.toString()) } catch (e: Exception) { url.error = e.message; return }
        if (!DownloadState.begin()) return
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(url.windowToken, 0)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        try {
            startForegroundService(Intent(this, DownloadService::class.java).putExtra("url", input).putExtra("mp3", mp3.isChecked)
                .putExtra("quality", listOf(360, 720, 1080)[quality.selectedItemPosition]))
        } catch (e: Exception) { DownloadState.publish(JobState(message = "Download konnte nicht gestartet werden.", error = e.message.orEmpty())) }
    }
    private fun render(state: JobState) {
        start.isEnabled = !state.busy; url.isEnabled = !state.busy; mp3.isEnabled = !state.busy; mp4.isEnabled = !state.busy; quality.isEnabled = !state.busy
        cancel.visibility = if (state.busy) View.VISIBLE else View.GONE
        cancel.isEnabled = state.busy
        status.text = state.message
        progress.visibility = if (state.busy) View.VISIBLE else View.GONE; progress.isIndeterminate = state.progress < 0; progress.progress = state.progress.coerceAtLeast(0)
        lastError = state.error
        details.visibility = if (state.error.isBlank()) View.GONE else View.VISIBLE
        if (!state.busy) refreshHistory()
    }
    private fun refreshHistory() {
        history.removeAllViews()
        val files = MediaFiles.history(this)
        if (files.isEmpty()) history.addView(TextView(this).apply { text = "Hier erscheinen deine gespeicherten Dateien."; textSize = 14f; setPadding(0, dp(8), 0, dp(12)) })
        files.forEach { file ->
            Button(this).apply {
                text = file.name; isAllCaps = false; gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                setOnClickListener {
                    try { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(file.uri), file.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                    catch (_: Exception) { Toast.makeText(this@MainActivity, "Datei fehlt oder keine passende Player-App installiert.", Toast.LENGTH_LONG).show() }
                }
            }.also { history.addView(it) }
        }
    }
}
