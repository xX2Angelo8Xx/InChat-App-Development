package com.chatgpt.speechnotes;

import android.content.Context;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Local-only persistence for biometric speaker embeddings. Enrollment audio is never stored. */
public final class SpeakerProfileStore {
    private final File file;

    public SpeakerProfileStore(Context context) {
        File dir = new File(context.getFilesDir(), "speaker-profiles");
        if (!dir.exists()) dir.mkdirs();
        file = new File(dir, "profiles-v1.json");
    }

    public synchronized List<SpeakerProfile> list() {
        if (!file.exists()) return Collections.emptyList();
        try {
            String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            JSONArray a = new JSONArray(json);
            ArrayList<SpeakerProfile> out = new ArrayList<>();
            for (int i = 0; i < a.length(); i++) out.add(SpeakerProfile.fromJson(a.getJSONObject(i)));
            return out;
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }

    public synchronized SpeakerProfile create(String name, List<float[]> raw) throws Exception {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) throw new IllegalArgumentException("Name fehlt");
        if (raw == null || raw.size() < 6) throw new IllegalArgumentException("Zu wenig verwertbare Sprachproben");
        ArrayList<float[]> samples = new ArrayList<>();
        for (float[] e : raw) if (e != null && e.length > 0) samples.add(e.clone());
        if (samples.size() < 6) throw new IllegalArgumentException("Zu wenig verwertbare Sprachproben");

        float[] initial = mean(samples);
        ArrayList<float[]> filtered = new ArrayList<>();
        for (float[] e : samples) if (SpeakerProfile.cosine(initial, e) >= 0.45f) filtered.add(e);
        if (filtered.size() < 5) throw new IllegalArgumentException("Stimme war während der Aufnahme zu inkonsistent");
        float[] centroid = mean(filtered);
        float sim = 0f;
        for (float[] e : filtered) sim += SpeakerProfile.cosine(centroid, e);
        float consistency = sim / filtered.size();

        // Keep a bounded diverse prototype set in addition to the centroid.
        ArrayList<float[]> prototypes = new ArrayList<>();
        int step = Math.max(1, filtered.size() / 10);
        for (int i = 0; i < filtered.size() && prototypes.size() < 12; i += step) prototypes.add(filtered.get(i));
        SpeakerProfile p = new SpeakerProfile(UUID.randomUUID().toString(), clean,
                System.currentTimeMillis(), centroid, prototypes, consistency);
        ArrayList<SpeakerProfile> all = new ArrayList<>(list());
        all.add(p); save(all); return p;
    }

    public synchronized void delete(String id) throws Exception {
        ArrayList<SpeakerProfile> all = new ArrayList<>();
        for (SpeakerProfile p : list()) if (!p.id.equals(id)) all.add(p);
        save(all);
    }

    private void save(List<SpeakerProfile> profiles) throws Exception {
        JSONArray a = new JSONArray();
        for (SpeakerProfile p : profiles) a.put(p.toJson());
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(a.toString().getBytes(StandardCharsets.UTF_8)); out.getFD().sync();
        }
        if (file.exists() && !file.delete()) throw new IllegalStateException("Alte Profildatei konnte nicht ersetzt werden");
        if (!tmp.renameTo(file)) throw new IllegalStateException("Profildatei konnte nicht installiert werden");
    }

    private static float[] mean(List<float[]> v) {
        int n = v.get(0).length; float[] out = new float[n]; int count = 0;
        for (float[] x : v) { if (x.length != n) continue; for (int i = 0; i < n; i++) out[i] += x[i]; count++; }
        for (int i = 0; i < n; i++) out[i] /= Math.max(1, count); SpeakerCluster.normalize(out); return out;
    }
}
