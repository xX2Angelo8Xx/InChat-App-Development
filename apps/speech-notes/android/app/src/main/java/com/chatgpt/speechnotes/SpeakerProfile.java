package com.chatgpt.speechnotes;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Persisted local voice identity. Embeddings stay on-device and no enrollment audio is retained. */
public final class SpeakerProfile {
    public final String id;
    public final String displayName;
    public final long createdAt;
    public final float[] centroid;
    public final List<float[]> prototypes;
    public final float consistency;

    public SpeakerProfile(String id, String displayName, long createdAt, float[] centroid,
                          List<float[]> prototypes, float consistency) {
        this.id = id;
        this.displayName = displayName;
        this.createdAt = createdAt;
        this.centroid = centroid.clone();
        ArrayList<float[]> copy = new ArrayList<>();
        for (float[] p : prototypes) copy.add(p.clone());
        this.prototypes = Collections.unmodifiableList(copy);
        this.consistency = consistency;
    }

    public float similarity(float[] embedding) { return cosine(centroid, embedding); }

    JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id); o.put("name", displayName); o.put("createdAt", createdAt);
        o.put("consistency", consistency); o.put("centroid", vectorToJson(centroid));
        JSONArray ps = new JSONArray(); for (float[] p : prototypes) ps.put(vectorToJson(p));
        o.put("prototypes", ps); return o;
    }

    static SpeakerProfile fromJson(JSONObject o) throws Exception {
        JSONArray ps = o.optJSONArray("prototypes"); ArrayList<float[]> prototypes = new ArrayList<>();
        if (ps != null) for (int i = 0; i < ps.length(); i++) prototypes.add(vectorFromJson(ps.getJSONArray(i)));
        return new SpeakerProfile(o.getString("id"), o.getString("name"), o.optLong("createdAt", 0L),
                vectorFromJson(o.getJSONArray("centroid")), prototypes, (float) o.optDouble("consistency", 0.0));
    }

    static float cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) return -1f;
        double dot = 0, aa = 0, bb = 0;
        for (int i = 0; i < a.length; i++) { dot += a[i] * b[i]; aa += a[i] * a[i]; bb += b[i] * b[i]; }
        if (aa <= 1e-12 || bb <= 1e-12) return -1f; return (float) (dot / Math.sqrt(aa * bb));
    }

    private static JSONArray vectorToJson(float[] v) { JSONArray a = new JSONArray(); for (float x : v) a.put((double) x); return a; }
    private static float[] vectorFromJson(JSONArray a) throws Exception { float[] v = new float[a.length()]; for (int i = 0; i < v.length; i++) v[i] = (float) a.getDouble(i); return v; }
}
