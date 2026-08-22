package com.chatgpt.speechnotes;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Guided ~50 s local voice enrollment. Raw enrollment audio exists only in memory. */
public final class SpeakerProfilesActivity extends Activity {
    private static final int SR = 16000;
    private static final int RECORD_SECONDS = 50;
    private static final int REQ_AUDIO = 301;
    private static final String PAGE1 = "Bitte lies diesen Text in deiner normalen Sprechstimme vor. Heute überprüfe ich eine technische Aufnahme, bei der deutliche Aussprache und natürliche Satzmelodie wichtig sind. Das Wetter kann ruhig oder windig sein, trotzdem soll die Stimme klar erkennbar bleiben. Zahlen wie sieben, vierzehn und neunundzwanzig sowie kurze Pausen helfen dabei, verschiedene Laute zuverlässig abzudecken.";
    private static final String PAGE2 = "Im zweiten Abschnitt wechseln längere und kürzere Sätze. Wie würdest du eine schwierige Entscheidung erklären, wenn mehrere Personen zuhören? Vielleicht beschreibst du zuerst die Ausgangslage, danach die wichtigsten Unterschiede und schließlich deine bevorzugte Lösung. Sprich weiter in normalem Tempo, ohne die Stimme absichtlich zu verändern. Nach diesem Abschnitt wird das lokale Sprecherprofil automatisch berechnet.";

    private LinearLayout root;
    private TextView text;
    private TextView status;
    private Button enroll;
    private volatile boolean enrolling;

    @Override protected void onCreate(Bundle b) { super.onCreate(b); showProfiles(); }

    private void showProfiles() {
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(Color.rgb(11,15,20));
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20),dp(24),dp(20),dp(30)); scroll.addView(root);
        root.addView(label("Sprecherprofile", 28, true));
        TextView sub = label("Profile bleiben ausschließlich auf diesem Gerät. Enrollment-Audio wird nach der Berechnung verworfen.", 14, false); sub.setTextColor(Color.LTGRAY); root.addView(sub);
        root.addView(space(18));
        List<SpeakerProfile> profiles = new SpeakerProfileStore(this).list();
        if (profiles.isEmpty()) root.addView(label("Noch keine Profile gespeichert.", 15, false));
        for (SpeakerProfile p : profiles) {
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0,dp(8),0,dp(8));
            TextView info = label(p.displayName + "\nQualität " + Math.round(p.consistency * 100f) + "% · " + p.prototypes.size() + " Proben", 15, false);
            row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Button del = new Button(this); del.setAllCaps(false); del.setText("Löschen"); del.setOnClickListener(v -> confirmDelete(p)); row.addView(del); root.addView(row);
        }
        root.addView(space(18));
        enroll = new Button(this); enroll.setAllCaps(false); enroll.setText("+ Sprecherprofil aufnehmen"); enroll.setOnClickListener(v -> beginEnrollment()); root.addView(enroll);
        setContentView(scroll);
    }

    private void beginEnrollment() {
        if (RecordingService.isRecording() || RecordingService.isTranscribing()) {
            Toast.makeText(this, "Sprecherprofile können nicht während einer laufenden Aufnahme erstellt werden.", Toast.LENGTH_LONG).show();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO); return;
        }
        startEnrollment();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startEnrollment();
        else if (requestCode == REQ_AUDIO) Toast.makeText(this, "Mikrofonberechtigung wird für das Sprecherprofil benötigt.", Toast.LENGTH_LONG).show();
    }

    private void startEnrollment() {
        if (enrolling) return; enrolling = true; root.removeAllViews();
        root.addView(label("Profil aufnehmen", 28, true));
        status = label("Seite 1 von 2 · Aufnahme startet …", 14, false); status.setTextColor(Color.rgb(100,220,190)); root.addView(status);
        root.addView(space(18)); text = label(PAGE1, 20, false); text.setLineSpacing(0f, 1.25f); root.addView(text);
        new Thread(this::recordAndEncode, "speaker-enrollment").start();
    }

    private void recordAndEncode() {
        AudioRecord rec = null;
        try {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Mikrofonberechtigung wurde während der Profilaufnahme entzogen");
            }
            int min = AudioRecord.getMinBufferSize(SR, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            rec = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SR, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, Math.max(min, SR * 2));
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("Mikrofon konnte nicht initialisiert werden");
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Mikrofonberechtigung wurde vor Aufnahmestart entzogen");
            }
            short[] all = new short[SR * RECORD_SECONDS]; short[] buf = new short[2048]; int pos = 0; boolean page2 = false;
            rec.startRecording(); long start = SystemClock.elapsedRealtime(); int lastShown = -1;
            while (pos < all.length) {
                int n = rec.read(buf, 0, Math.min(buf.length, all.length - pos));
                if (n < 0) throw new IllegalStateException("AudioRecord Fehler " + n); if (n == 0) continue;
                System.arraycopy(buf, 0, all, pos, n); pos += n; long elapsed = SystemClock.elapsedRealtime() - start;
                if (!page2 && elapsed >= 25000) { page2 = true; runOnUiThread(() -> { status.setText("Seite 2 von 2 · weiter sprechen"); text.setText(PAGE2); }); }
                int sec = Math.min(RECORD_SECONDS, (int)(elapsed / 1000));
                if (sec != lastShown && sec % 5 == 0) { lastShown = sec; final int shown = sec; runOnUiThread(() -> status.setText((shown < 25 ? "Seite 1 von 2" : "Seite 2 von 2") + " · " + shown + "/50 s")); }
            }
            rec.stop(); rec.release(); rec = null; runOnUiThread(() -> status.setText("Profil wird lokal berechnet …"));
            ArrayList<float[]> embeddings = new ArrayList<>();
            try (SpeakerEmbeddingModel model = new SpeakerEmbeddingModel(this)) {
                int win = SR * 3;
                for (int s = 0; s + win <= all.length; s += win) {
                    short[] chunk = new short[win]; System.arraycopy(all, s, chunk, 0, win);
                    if (rms(chunk) < 0.012) continue; embeddings.add(model.embed(chunk));
                }
            }
            java.util.Arrays.fill(all, (short)0);
            if (embeddings.size() < 8) throw new IllegalStateException("Zu wenig verwertbare Sprache. Bitte erneut aufnehmen.");
            runOnUiThread(() -> askName(embeddings));
        } catch (Throwable t) {
            if (rec != null) { try { rec.stop(); } catch (Throwable ignored) {} try { rec.release(); } catch (Throwable ignored) {} }
            runOnUiThread(() -> { enrolling = false; Toast.makeText(this, "Profilfehler: " + t.getMessage(), Toast.LENGTH_LONG).show(); showProfiles(); });
        }
    }

    private void askName(List<float[]> embeddings) {
        EditText input = new EditText(this); input.setHint("Name");
        new AlertDialog.Builder(this).setTitle("Profil erfolgreich aufgenommen").setMessage("Wie heißt diese Person?").setView(input).setCancelable(false)
                .setPositiveButton("Speichern", (d,w) -> { try { new SpeakerProfileStore(this).create(input.getText().toString(), embeddings); enrolling = false; showProfiles(); } catch (Throwable t) { enrolling = false; Toast.makeText(this, t.getMessage(), Toast.LENGTH_LONG).show(); showProfiles(); } })
                .setNegativeButton("Verwerfen", (d,w) -> { enrolling = false; showProfiles(); }).show();
    }

    private void confirmDelete(SpeakerProfile p) {
        new AlertDialog.Builder(this).setTitle(p.displayName + " löschen?").setMessage("Das lokale Stimmprofil wird vollständig entfernt.")
                .setPositiveButton("Löschen", (d,w) -> { try { new SpeakerProfileStore(this).delete(p.id); showProfiles(); } catch (Throwable t) { Toast.makeText(this,t.getMessage(),Toast.LENGTH_LONG).show(); } })
                .setNegativeButton("Abbrechen", null).show();
    }

    private static double rms(short[] x) { double s=0; for(short v:x){double f=v/32768.0;s+=f*f;} return Math.sqrt(s/Math.max(1,x.length)); }
    private TextView label(String s,int sp,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(Color.rgb(242,246,250));if(bold)v.setTypeface(null,1);return v;}
    private TextView space(int h){TextView v=new TextView(this);v.setHeight(dp(h));return v;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
