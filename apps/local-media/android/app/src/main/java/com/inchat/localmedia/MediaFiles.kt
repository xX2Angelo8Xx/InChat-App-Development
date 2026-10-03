package com.inchat.localmedia

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SavedMedia(val name: String, val uri: String, val mime: String)

object MediaFiles {
    fun publish(context: Context, source: File): SavedMedia {
        val mime = if (source.extension == "mp3") "audio/mpeg" else "video/mp4"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, source.name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/Local Media")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) { "Download-Ordner nicht verfügbar." }
        try {
            requireNotNull(resolver.openOutputStream(uri)).use { out -> source.inputStream().use { it.copyTo(out) } }
            values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0)
            check(resolver.update(uri, values, null, null) == 1)
            val saved = SavedMedia(source.name, uri.toString(), mime)
            val old = history(context)
            val array = JSONArray()
            (listOf(saved) + old).take(20).forEach {
                array.put(JSONObject().put("name", it.name).put("uri", it.uri).put("mime", it.mime))
            }
            check(context.getSharedPreferences("history", Context.MODE_PRIVATE).edit().putString("items", array.toString()).commit())
            return saved
        } catch (e: Exception) { resolver.delete(uri, null, null); throw e }
    }
    fun history(context: Context): List<SavedMedia> = try {
        val array = JSONArray(context.getSharedPreferences("history", Context.MODE_PRIVATE).getString("items", "[]"))
        (0 until array.length()).map { i -> array.getJSONObject(i).let { SavedMedia(it.getString("name"), it.getString("uri"), it.getString("mime")) } }
    } catch (_: Exception) { emptyList() }
    fun recover(context: Context) {
        // A process death can leave an unfinished MediaStore copy; remove only our pending rows.
        context.contentResolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            "${MediaStore.Downloads.IS_PENDING} = 1 AND ${MediaStore.Downloads.RELATIVE_PATH} = ?",
            arrayOf("Download/Local Media/"))
    }
}
