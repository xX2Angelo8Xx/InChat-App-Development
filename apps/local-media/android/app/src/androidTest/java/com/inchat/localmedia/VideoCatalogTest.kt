package com.inchat.localmedia

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoCatalogTest {
    private fun fixture() = InstrumentationRegistry.getInstrumentation().context.assets
        .open("catalog-fixture.json").bufferedReader().use { it.readText() }
    @Test fun formatsReflectSourceAndExcludeUnsupportedOrProtectedTracks() {
        val video = VideoCatalog.parse("https://www.youtube.com/watch?v=BaW_jenozKc", fixture())
        assertEquals("Testvideo · verfügbare Qualitäten", video.title)
        assertEquals(listOf("251", "140"), video.audio.map { it.id })
        assertEquals(listOf("22", "299"), video.video.map { it.id })
        assertTrue(video.video.first().hasAudio); assertFalse(video.video.last().hasAudio)
        assertEquals(125, video.duration)
    }
    @Test fun videoOnlySourcesAreNotOfferedWithoutAnAudioTrack() {
        val data = JSONObject(fixture())
        val source = data.getJSONArray("formats")
        val formats = org.json.JSONArray()
        formats.put(source.getJSONObject(3))
        data.put("formats", formats)
        try { VideoCatalog.parse("https://www.youtube.com/watch?v=BaW_jenozKc", data.toString()); fail() }
        catch (_: IllegalArgumentException) { }
    }
}
