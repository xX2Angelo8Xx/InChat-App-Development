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
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.*

class MainActivity : Activity() {
    private lateinit var url: EditText
    private lateinit var paste: Button
    private lateinit var search: Button
    private lateinit var preview: LinearLayout
    private lateinit var thumbnail: ImageView
    private lateinit var videoTitle: TextView
    private lateinit var duration: TextView
    private lateinit var mp3: RadioButton
    private lateinit var mp4: RadioButton
    private lateinit var quality: Spinner
    private lateinit var formatHint: TextView
    private lateinit var start: Button
    private lateinit var cancel: Button
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var details: Button
    private lateinit var history: LinearLayout
    private var lastError = ""
    private var shownVideo: VideoDetails? = null
    private var audioIndex = 0
    private var videoIndex = 0
    private val observer: (JobState) -> Unit = { render() }
    private val searchObserver: (SearchState) -> Unit = { render() }
    private val green = Color.rgb(23, 107, 89)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun label(text: String, size: Float = 16f, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(Color.rgb(25, 42, 37)); setPadding(0, dp(7), 0, dp(7))
        if (bold) setTypeface(null, Typeface.BOLD)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(245, 247, 244)) }
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
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(16), dp(22), dp(20)) }
        root.addView(scroll); scroll.addView(column); setContentView(root); root.requestApplyInsets()
        column.addView(label("LOCAL MEDIA", 12f, true).apply { setTextColor(green) })
        column.addView(label("Link suchen.\nFormat wählen.", 28f, true))
        column.addView(label("MP3 oder MP4 · direkt auf deinem Gerät", 14f))
        url = EditText(this).apply {
            hint = "YouTube-Link einfügen"; textSize = 16f; setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(12).toFloat(); setStroke(dp(1), Color.rgb(209, 222, 214)) }
        }; column.addView(url, LinearLayout.LayoutParams(-1, dp(54)))
        val actions = LinearLayout(this)
        paste = Button(this).apply {
            text = "Einfügen"; isAllCaps = false
            setOnClickListener {
                val clip = (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
                val text = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(this@MainActivity).toString() else ""
                if (text.isNotBlank()) url.setText(extractLink(text)) else Toast.makeText(this@MainActivity, "Zwischenablage ist leer.", Toast.LENGTH_SHORT).show()
            }
        }
        search = Button(this).apply {
            text = "Suchen"; isAllCaps = false; setTextColor(Color.WHITE); backgroundTintList = android.content.res.ColorStateList.valueOf(green)
            setOnClickListener {
                val input = try { DownloadOptions.normalize(url.text.toString()) } catch (e: Exception) { url.error = e.message; return@setOnClickListener }
                hideKeyboard(); DownloadState.publish(JobState()); VideoSearch.start(this@MainActivity, input)
            }
        }
        actions.addView(paste, LinearLayout.LayoutParams(0, dp(52), 1f))
        actions.addView(search, LinearLayout.LayoutParams(0, dp(52), 1f)); column.addView(actions)
        preview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; visibility = View.GONE; setPadding(dp(12), dp(10), dp(12), dp(12))
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(14).toFloat() }
        }; column.addView(preview)
        thumbnail = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER; contentDescription = "Video-Thumbnail"; setBackgroundColor(Color.rgb(233, 239, 235)) }
        preview.addView(thumbnail, LinearLayout.LayoutParams(-1, dp(158)))
        videoTitle = label("", 19f, true); preview.addView(videoTitle)
        duration = label("", 12f); preview.addView(duration)
        val format = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        mp3 = RadioButton(this).apply { id = View.generateViewId(); text = "MP3 · Audio"; isChecked = true }
        mp4 = RadioButton(this).apply { id = View.generateViewId(); text = "MP4 · Video" }
        format.addView(mp3, RadioGroup.LayoutParams(0, dp(48), 1f)); format.addView(mp4, RadioGroup.LayoutParams(0, dp(48), 1f)); preview.addView(format)
        quality = Spinner(this); preview.addView(quality, LinearLayout.LayoutParams(-1, dp(48)))
        formatHint = label("", 12f); preview.addView(formatHint)
        format.setOnCheckedChangeListener { _, _ -> updateChoices() }
        quality.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (mp3.isChecked) audioIndex = position else videoIndex = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
        start = Button(this).apply {
            text = "Herunterladen"; isAllCaps = false; setTextColor(Color.WHITE); backgroundTintList = android.content.res.ColorStateList.valueOf(green)
            setOnClickListener { startDownload() }
        }; preview.addView(start, LinearLayout.LayoutParams(-1, dp(54)))
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; visibility = View.GONE }; column.addView(progress)
        status = label("Link einfügen und auf Suchen drücken.", 14f); column.addView(status)
        details = Button(this).apply {
            text = "Fehlerdetails anzeigen"; visibility = View.GONE; isAllCaps = false
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity).setTitle("Fehlerdetails").setMessage(lastError).setPositiveButton("Schließen", null)
                    .setNeutralButton("Kopieren") { _, _ -> (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(android.content.ClipData.newPlainText("Local Media Fehler", lastError)) }.show()
            }
        }; column.addView(details)
        cancel = Button(this).apply {
            text = "Abbrechen"; visibility = View.GONE; isAllCaps = false
            setOnClickListener {
                if (VideoSearch.current.busy) VideoSearch.clear()
                else startService(Intent(this@MainActivity, DownloadService::class.java).setAction("cancel"))
                isEnabled = false
            }
        }; column.addView(cancel)
        column.addView(label("Letzte Downloads", 18f, true))
        history = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; column.addView(history)
        column.addView(label("Downloads / Local Media · Verarbeitung lokal", 12f))
        if (savedInstanceState != null) {
            url.setText(savedInstanceState.getString("url", ""))
            audioIndex = savedInstanceState.getInt("audio", 0); videoIndex = savedInstanceState.getInt("video", 0)
            if (!savedInstanceState.getBoolean("mp3", true)) mp4.isChecked = true
        } else receive(intent)
        url.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val key = normalizedInput()
                if (VideoSearch.current.query.isNotEmpty() && key != VideoSearch.current.query) VideoSearch.clear()
                render()
            }
            override fun afterTextChanged(s: Editable?) { }
        })
        render()
    }
    private fun normalizedInput(): String = try { DownloadOptions.normalize(url.text.toString()) } catch (_: Exception) { "" }
    private fun extractLink(text: String) = Regex("https://[^\\s<>]+").find(text)?.value?.trimEnd('.', ',', ')') ?: text.trim()
    private fun receive(value: Intent) { if (value.action == Intent.ACTION_SEND) url.setText(extractLink(value.getStringExtra(Intent.EXTRA_TEXT).orEmpty())) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); receive(intent) }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("url", url.text.toString()); outState.putBoolean("mp3", mp3.isChecked)
        outState.putInt("audio", audioIndex); outState.putInt("video", videoIndex); super.onSaveInstanceState(outState)
    }
    override fun onStart() { super.onStart(); DownloadState.observe(observer); VideoSearch.observe(searchObserver) }
    override fun onStop() { DownloadState.remove(observer); VideoSearch.remove(searchObserver); super.onStop() }
    private fun hideKeyboard() { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(url.windowToken, 0) }
    private fun updateChoices() {
        val info = shownVideo ?: return
        val choices = if (mp3.isChecked) info.audio else info.video
        quality.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, choices.map { it.label })
        quality.setSelection((if (mp3.isChecked) audioIndex else videoIndex).coerceIn(0, (choices.size - 1).coerceAtLeast(0)))
        formatHint.text = if (mp3.isChecked) "Verfügbare Audioquelle · MP3-Konvertierung mit 192 kbit/s. Die Quelle bestimmt die Qualität."
            else "Verfügbare MP4-Auflösungen · H.264 mit Ton."
        start.isEnabled = choices.isNotEmpty() && !DownloadState.current.busy && !VideoSearch.current.busy
    }
    private fun startDownload() {
        val info = shownVideo ?: return
        if (info.url != normalizedInput()) { VideoSearch.clear(); return }
        val choice = (if (mp3.isChecked) info.audio else info.video).getOrNull(quality.selectedItemPosition) ?: return
        val audio = if (!mp3.isChecked && !choice.hasAudio) info.audio.firstOrNull() else null
        if (!mp3.isChecked && !choice.hasAudio && audio == null) return
        if (!DownloadState.begin()) return
        hideKeyboard()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        try {
            startForegroundService(Intent(this, DownloadService::class.java).putExtra("url", info.url).putExtra("mp3", mp3.isChecked)
                .putExtra("sourceId", choice.id).putExtra("audioId", audio?.id.orEmpty()).putExtra("title", info.title))
        } catch (e: Exception) { DownloadState.publish(JobState(message = "Download konnte nicht gestartet werden.", error = e.message.orEmpty())) }
    }
    private fun render() {
        val job = DownloadState.current
        val found = VideoSearch.current
        val busy = job.busy || found.busy
        val info = found.details?.takeIf { found.query == normalizedInput() }
        url.isEnabled = !busy; paste.isEnabled = !busy; search.isEnabled = !busy && url.text.isNotBlank()
        preview.visibility = if (info == null) View.GONE else View.VISIBLE
        if (shownVideo != info) {
            val first = shownVideo == null
            shownVideo = info
            if (info != null) {
                videoTitle.text = info.title
                duration.text = if (info.duration > 0) "${info.duration / 60}:${(info.duration % 60).toString().padStart(2, '0')} · Vorschau" else "Video-Vorschau"
                if (!first) { audioIndex = 0; videoIndex = 0 }
                if (info.audio.isEmpty()) mp4.isChecked = true
                else if (info.video.isEmpty()) mp3.isChecked = true
                updateChoices()
            }
        }
        thumbnail.setImageBitmap(found.thumbnail)
        thumbnail.contentDescription = if (found.thumbnail == null) "Thumbnail noch nicht verfügbar" else "Thumbnail: ${info?.title.orEmpty()}"
        mp3.isEnabled = !busy && info?.audio?.isNotEmpty() == true; mp4.isEnabled = !busy && info?.video?.isNotEmpty() == true
        quality.isEnabled = !busy
        start.isEnabled = !busy && info != null && (if (mp3.isChecked) info.audio else info.video).isNotEmpty()
        cancel.visibility = if (busy) View.VISIBLE else View.GONE; cancel.isEnabled = busy
        status.text = when {
            job.busy -> job.message
            found.busy -> "Videoinformationen werden gesucht …"
            found.error.isNotBlank() -> "Suche fehlgeschlagen."
            job.error.isNotBlank() -> job.message
            info != null -> if (job.message.startsWith("Gespeichert:") || job.message == "Download abgebrochen.") job.message else "Video gefunden. Format und Qualität wählen."
            else -> "Link einfügen und auf Suchen drücken."
        }
        progress.visibility = if (busy) View.VISIBLE else View.GONE; progress.isIndeterminate = !job.busy || job.progress < 0
        progress.progress = job.progress.coerceAtLeast(0)
        lastError = if (found.error.isNotBlank()) found.error else job.error
        details.visibility = if (lastError.isBlank()) View.GONE else View.VISIBLE
        if (!job.busy) refreshHistory()
    }
    private fun refreshHistory() {
        history.removeAllViews()
        val files = MediaFiles.history(this)
        if (files.isEmpty()) history.addView(label("Hier erscheinen deine gespeicherten Dateien.", 13f))
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
