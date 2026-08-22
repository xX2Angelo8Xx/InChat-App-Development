from pathlib import Path
import re


def rep(text, old, new, label):
    if old not in text:
        raise SystemExit(f'v2.1 anchor missing: {label}')
    return text.replace(old, new, 1)

# -----------------------------------------------------------------------------
# Main UI: configurable 1..10 speakers plus local profile management.
# -----------------------------------------------------------------------------
p = Path('SpeechNotes/app/src/main/java/com/chatgpt/speechnotes/MainActivity.java')
s = p.read_text()
s = rep(s, '    private Switch speakerSwitch;\n', '    private Switch speakerSwitch;\n    private Spinner speakerSpinner;\n', 'speaker spinner field')
old = '''        LinearLayout speakerCard = toggleCard("Sprechertrennung",\n                "On-device CAM++ · automatisch · maximal 3 Sprecher");\n        speakerSwitch = new Switch(this);\n        speakerSwitch.setChecked(prefs.getBoolean("speaker_diarization", true));\n        speakerCard.addView(speakerSwitch);\n        root.addView(speakerCard, matchWrap());\n'''
new = '''        LinearLayout speakerCard = toggleCard("Sprechertrennung",\n                "On-device CAM++ · Profile werden lokal erkannt");\n        speakerSwitch = new Switch(this);\n        speakerSwitch.setChecked(prefs.getBoolean("speaker_diarization", true));\n        speakerCard.addView(speakerSwitch);\n        root.addView(speakerCard, matchWrap());\n\n        root.addView(space(8));\n        LinearLayout countCard = toggleCard("Maximale Teilnehmer", "Dynamisches Clustering · 1 bis 10 Sprecher");\n        speakerSpinner = new Spinner(this);\n        String[] speakerCounts = {"1","2","3","4","5","6","7","8","9","10"};\n        speakerSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, speakerCounts));\n        int savedMaxSpeakers = Math.max(1, Math.min(10, prefs.getInt("max_speakers", 3)));\n        speakerSpinner.setSelection(savedMaxSpeakers - 1);\n        countCard.addView(speakerSpinner);\n        root.addView(countCard, matchWrap());\n\n        root.addView(space(8));\n        Button profilesButton = new Button(this);\n        profilesButton.setAllCaps(false);\n        profilesButton.setText("Sprecherprofile verwalten / aufnehmen");\n        profilesButton.setTextColor(TEXT);\n        profilesButton.setBackground(roundRect(CARD_2, 15));\n        profilesButton.setOnClickListener(v -> startActivity(new Intent(this, SpeakerProfilesActivity.class)));\n        root.addView(profilesButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));\n'''
s = rep(s, old, new, 'speaker controls')
old = '''        boolean useSpeakers = speakerSwitch != null && speakerSwitch.isChecked();\n        boolean save = wavSwitch.isChecked();\n        prefs.edit().putInt("model_v2", idx)\n                .putBoolean("wav", save)\n                .putBoolean("aviation_prompt", useAviation)\n                .putBoolean("speaker_diarization", useSpeakers).apply();'''
new = '''        boolean useSpeakers = speakerSwitch != null && speakerSwitch.isChecked();\n        int maxSpeakers = speakerSpinner == null ? 3 : Math.max(1, Math.min(10, speakerSpinner.getSelectedItemPosition() + 1));\n        boolean save = wavSwitch.isChecked();\n        prefs.edit().putInt("model_v2", idx)\n                .putBoolean("wav", save)\n                .putBoolean("aviation_prompt", useAviation)\n                .putBoolean("speaker_diarization", useSpeakers)\n                .putInt("max_speakers", maxSpeakers).apply();'''
s = rep(s, old, new, 'persist max speakers')
old = '''                .putExtra(RecordingService.EXTRA_AVIATION_PROMPT, useAviation)\n                .putExtra(RecordingService.EXTRA_DIARIZATION, useSpeakers);'''
new = '''                .putExtra(RecordingService.EXTRA_AVIATION_PROMPT, useAviation)\n                .putExtra(RecordingService.EXTRA_DIARIZATION, useSpeakers)\n                .putExtra(RecordingService.EXTRA_MAX_SPEAKERS, maxSpeakers);'''
s = rep(s, old, new, 'max speakers intent')
s = s.replace('if (speakerSwitch != null) speakerSwitch.setEnabled(false);', 'if (speakerSwitch != null) speakerSwitch.setEnabled(false); if (speakerSpinner != null) speakerSpinner.setEnabled(false);')
s = s.replace('if (speakerSwitch != null) speakerSwitch.setEnabled(true);', 'if (speakerSwitch != null) speakerSwitch.setEnabled(true); if (speakerSpinner != null) speakerSpinner.setEnabled(true);')
p.write_text(s)

# -----------------------------------------------------------------------------
# Recording service: pass the configured cluster bound into the v2 engine.
# -----------------------------------------------------------------------------
p = Path('SpeechNotes/app/src/main/java/com/chatgpt/speechnotes/RecordingService.java')
s = p.read_text()
s = rep(s, '    public static final String EXTRA_DIARIZATION = "diarization";\n',
        '    public static final String EXTRA_DIARIZATION = "diarization";\n    public static final String EXTRA_MAX_SPEAKERS = "max_speakers";\n', 'service max extra')
s = rep(s, '    private boolean speakerDiarization = true;\n', '    private boolean speakerDiarization = true;\n    private int maxSpeakers = 3;\n', 'service max field')
s = rep(s, '            speakerDiarization = intent.getBooleanExtra(EXTRA_DIARIZATION, true);\n            startRecording();',
        '            speakerDiarization = intent.getBooleanExtra(EXTRA_DIARIZATION, true);\n            maxSpeakers = Math.max(1, Math.min(10, intent.getIntExtra(EXTRA_MAX_SPEAKERS, 3)));\n            startRecording();', 'read max speakers')
s = rep(s, '                diarizer = new SpeakerDiarizationEngine(this);\n                CrashDiagnostics.mark(this, "speaker:runtime_ready load_ms="\n                        + (SystemClock.elapsedRealtime() - speakerLoadStart) + " max=3");',
        '                diarizer = new SpeakerDiarizationEngine(this, new SpeakerDiarizationEngine.Config(maxSpeakers));\n                CrashDiagnostics.mark(this, "speaker:runtime_ready load_ms="\n                        + (SystemClock.elapsedRealtime() - speakerLoadStart) + " max=" + maxSpeakers\n                        + " profiles=" + diarizer.profileCount());', 'engine config')
s = s.replace(' + diarizer.speakerCount() + "/3 · Emb "', ' + diarizer.speakerCount() + "/" + maxSpeakers + " · Emb "')
p.write_text(s)

# -----------------------------------------------------------------------------
# Diarization v2: 1.5 s / 0.5 s sliding CAM++ observations, dynamic 1..10 clusters,
# conservative open-set profile matching and sequence smoothing.
# -----------------------------------------------------------------------------
engine = r'''package com.chatgpt.speechnotes;

import android.content.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SpeakerDiarizationEngine implements AutoCloseable {
    private static final int SAMPLE_RATE = 16000;
    private static final int FRAME = 480;
    private static final int MIN_SPEECH = 16000;
    private static final int WINDOW = 24000; // 1.5 s
    private static final int HOP = 8000;      // 0.5 s
    private static final int MERGE_GAP = 9600;
    private static final float CREATE_THRESHOLD = 0.48f;
    private static final float PROFILE_THRESHOLD = 0.62f;
    private static final float PROFILE_MARGIN = 0.04f;
    private static final float SWITCH_PENALTY = 0.08f;

    public static final class Config {
        public final int maxSpeakers;
        public Config(int maxSpeakers) { this.maxSpeakers = Math.max(1, Math.min(10, maxSpeakers)); }
    }

    public static final class SpeakerTurn {
        public final long startSample, endSample;
        public final int speakerId;
        public final float confidence;
        SpeakerTurn(long s,long e,int id,float c){startSample=s;endSample=e;speakerId=id;confidence=c;}
    }

    private static final class Observation {
        final long start,end; final float[] embedding; int speakerId; float similarity;
        Observation(long s,long e,float[] x,int id,float sim){start=s;end=e;embedding=x;speakerId=id;similarity=sim;}
        long center(){return start+(end-start)/2;}
    }
    private static final class Assignment { final int id; final float sim; Assignment(int i,float s){id=i;sim=s;} }
    private static final class Range { final int start,end; Range(int s,int e){start=s;end=e;} }

    private final SpeakerEmbeddingModel encoder;
    private final Config config;
    private final List<SpeakerProfile> profiles;
    private final List<SpeakerCluster> clusters = new ArrayList<>();
    private final List<Observation> observations = new ArrayList<>();
    private final List<SpeakerTurn> turns = new ArrayList<>();
    private long processedUntilSample, embeddingWallMs;
    private int embeddingRuns;

    public SpeakerDiarizationEngine(Context c) throws Exception { this(c, new Config(3)); }
    public SpeakerDiarizationEngine(Context c, Config cfg) throws Exception {
        config=cfg; encoder=new SpeakerEmbeddingModel(c); profiles=new SpeakerProfileStore(c).list();
    }
    public synchronized int speakerCount(){return clusters.size();}
    public synchronized int profileCount(){return profiles.size();}
    public synchronized long embeddingWallMs(){return embeddingWallMs;}
    public synchronized int embeddingRuns(){return embeddingRuns;}
    public synchronized List<SpeakerCluster> clusters(){return Collections.unmodifiableList(new ArrayList<>(clusters));}
    public synchronized List<SpeakerTurn> turns(){return Collections.unmodifiableList(new ArrayList<>(turns));}

    public synchronized void analyzeWindow(short[] pcm,long absoluteStartSample) throws Exception {
        if(pcm==null||pcm.length==0)return;
        long absoluteEnd=absoluteStartSample+pcm.length;
        int skip=(int)Math.max(0L,Math.min((long)pcm.length,processedUntilSample-absoluteStartSample));
        if(skip>=pcm.length)return;
        short[] fresh=Arrays.copyOfRange(pcm,skip,pcm.length); long base=absoluteStartSample+skip;
        for(Range r:detectSpeech(fresh)){
            int len=r.end-r.start;
            if(len<MIN_SPEECH)continue;
            if(len<=WINDOW){processChunk(fresh,r.start,r.end,base);continue;}
            int last=-1;
            for(int st=r.start;st+WINDOW<=r.end;st+=HOP){processChunk(fresh,st,st+WINDOW,base);last=st;}
            int tail=r.end-WINDOW;
            if(tail>last+HOP/2)processChunk(fresh,tail,r.end,base);
        }
        processedUntilSample=Math.max(processedUntilSample,absoluteEnd);
        smoothSequence(); rebuildTurns();
    }

    private void processChunk(short[] pcm,int start,int end,long base) throws Exception {
        short[] speech=Arrays.copyOfRange(pcm,start,end); long wall=android.os.SystemClock.elapsedRealtime();
        float[] e=encoder.embed(speech); embeddingWallMs+=android.os.SystemClock.elapsedRealtime()-wall; embeddingRuns++;
        Assignment a=assign(e); observations.add(new Observation(base+start,base+end,e,a.id,a.sim));
    }

    private Assignment assign(float[] e){
        if(clusters.isEmpty()){clusters.add(new SpeakerCluster(1,e));return new Assignment(1,1f);}
        SpeakerCluster best=null;float sim=-1f;
        for(SpeakerCluster c:clusters){float x=c.similarity(e);if(x>sim){sim=x;best=c;}}
        if(sim<CREATE_THRESHOLD && clusters.size()<config.maxSpeakers){
            SpeakerCluster c=new SpeakerCluster(clusters.size()+1,e);clusters.add(c);return new Assignment(c.id(),1f);
        }
        if(best==null)return new Assignment(0,0f); best.update(e); return new Assignment(best.id(),sim);
    }

    public synchronized void refineGlobal(){
        if(observations.size()<2||clusters.isEmpty()){rebuildTurns();return;}
        for(int pass=0;pass<3;pass++){
            smoothSequence();
            List<float[]> sums=new ArrayList<>();int[] count=new int[clusters.size()];
            for(SpeakerCluster c:clusters)sums.add(new float[c.centroidCopy().length]);
            for(Observation o:observations){if(o.speakerId<=0||o.speakerId>clusters.size())continue;int k=o.speakerId-1;float[] sum=sums.get(k);for(int j=0;j<sum.length;j++)sum[j]+=o.embedding[j];count[k]++;}
            for(int i=0;i<clusters.size();i++){if(count[i]==0)continue;float[] m=sums.get(i);for(int j=0;j<m.length;j++)m[j]/=count[i];clusters.get(i).resetTo(m,count[i]);}
        }
        smoothSequence();rebuildTurns();
    }

    private void smoothSequence(){
        if(observations.isEmpty()||clusters.isEmpty())return;
        observations.sort(Comparator.comparingLong(o->o.start)); int n=observations.size(),k=clusters.size();
        float[][] dp=new float[n][k];int[][] prev=new int[n][k];
        for(int c=0;c<k;c++){dp[0][c]=clusters.get(c).similarity(observations.get(0).embedding);prev[0][c]=-1;}
        for(int i=1;i<n;i++)for(int c=0;c<k;c++){
            float emit=clusters.get(c).similarity(observations.get(i).embedding);float best=-999f;int bp=0;
            boolean close=observations.get(i).start-observations.get(i-1).end<2L*SAMPLE_RATE;
            for(int pc=0;pc<k;pc++){float score=dp[i-1][pc]-((close&&pc!=c)?SWITCH_PENALTY:0f);if(score>best){best=score;bp=pc;}}
            dp[i][c]=best+emit;prev[i][c]=bp;
        }
        int state=0;for(int c=1;c<k;c++)if(dp[n-1][c]>dp[n-1][state])state=c;
        for(int i=n-1;i>=0;i--){Observation o=observations.get(i);o.speakerId=clusters.get(state).id();o.similarity=clusters.get(state).similarity(o.embedding);state=prev[i][state];if(state<0)break;}
    }

    private void rebuildTurns(){
        turns.clear(); if(observations.isEmpty())return; observations.sort(Comparator.comparingLong(o->o.start));
        long turnStart=observations.get(0).start;int speaker=observations.get(0).speakerId;float conf=confidence(observations.get(0).similarity);
        for(int i=1;i<observations.size();i++){
            Observation a=observations.get(i-1),b=observations.get(i);float c=confidence(b.similarity);
            if(b.speakerId!=speaker){long boundary=(a.center()+b.center())/2;turns.add(new SpeakerTurn(turnStart,boundary,speaker,conf));turnStart=boundary;speaker=b.speakerId;conf=c;}
            else conf=Math.min(conf,c);
        }
        Observation last=observations.get(observations.size()-1);turns.add(new SpeakerTurn(turnStart,last.end,speaker,conf));
        // Merge same-speaker turns separated only by tiny VAD gaps.
        for(int i=turns.size()-2;i>=0;i--){SpeakerTurn a=turns.get(i),b=turns.get(i+1);if(a.speakerId==b.speakerId&&b.startSample-a.endSample<=MERGE_GAP){turns.set(i,new SpeakerTurn(a.startSample,b.endSample,a.speakerId,Math.min(a.confidence,b.confidence)));turns.remove(i+1);}}
    }

    public synchronized int speakerForInterval(long s,long e){long best=0;int id=0;for(SpeakerTurn t:turns){long o=Math.max(0,Math.min(e,t.endSample)-Math.max(s,t.startSample));if(o>best){best=o;id=t.speakerId;}}return id;}
    public synchronized int speakerAtSample(long sample){for(SpeakerTurn t:turns)if(sample>=t.startSample&&sample<t.endSample)return t.speakerId;return speakerForInterval(sample,sample+1);}
    public synchronized String labelForSpeaker(int id){
        if(id<=0)return "Sprecher ?"; SpeakerCluster c=null;for(SpeakerCluster x:clusters)if(x.id()==id){c=x;break;} if(c==null)return "Sprecher "+id;
        SpeakerProfile best=null;float b=-1f,second=-1f;float[] center=c.centroidCopy();
        for(SpeakerProfile p:profiles){float sim=p.similarity(center);if(sim>b){second=b;b=sim;best=p;}else if(sim>second)second=sim;}
        if(best!=null&&b>=PROFILE_THRESHOLD&&(second<0||b-second>=PROFILE_MARGIN))return best.displayName;
        return "Sprecher "+id;
    }

    private static float confidence(float sim){return Math.max(0f,Math.min(1f,(sim-0.25f)/0.55f));}
    private static List<Range> detectSpeech(short[] pcm){
        int frames=pcm.length/FRAME;if(frames==0)return Collections.emptyList();double[] db=new double[frames],sorted=new double[frames];
        for(int f=0;f<frames;f++){double sum=0;int base=f*FRAME;for(int i=0;i<FRAME;i++){double x=pcm[base+i]/32768.0;sum+=x*x;}double rms=Math.sqrt(sum/FRAME);db[f]=20*Math.log10(Math.max(rms,1e-7));sorted[f]=db[f];}
        Arrays.sort(sorted);double noise=sorted[Math.min(sorted.length-1,Math.max(0,sorted.length/5))];double threshold=Math.max(-43.0,Math.min(-25.0,noise+9.0));
        int hangFrames=4,start=-1,hang=0;List<Range> out=new ArrayList<>();
        for(int f=0;f<frames;f++){boolean speech=db[f]>=threshold;if(speech){if(start<0)start=Math.max(0,f-1);hang=hangFrames;}else if(start>=0){if(hang>0)hang--;else{int ss=start*FRAME,ee=Math.min(pcm.length,f*FRAME);if(ee-ss>=MIN_SPEECH)out.add(new Range(ss,ee));start=-1;}}}
        if(start>=0){int ss=start*FRAME;if(pcm.length-ss>=MIN_SPEECH)out.add(new Range(ss,pcm.length));}return out;
    }
    @Override public synchronized void close() throws Exception {encoder.close();}
}
'''
Path('SpeechNotes/app/src/main/java/com/chatgpt/speechnotes/SpeakerDiarizationEngine.java').write_text(engine)

# -----------------------------------------------------------------------------
# Fusion v2: interpolate word timing inside Whisper segments and split at every
# detected speaker-turn boundary. This fixes the old one-speaker-per-segment rule
# without enabling experimental Whisper token timestamps.
# -----------------------------------------------------------------------------
fusion = r'''package com.chatgpt.speechnotes;

import java.util.ArrayList;
import java.util.List;

public final class SpeakerTranscriptFusion {
    private static final long SAMPLES_PER_WHISPER_TICK = 160L;
    private static final class RawSegment { final long start,end; final String text; RawSegment(long s,long e,String t){start=s;end=e;text=t;} }
    private static final class Block { final int speakerId; final String label; final StringBuilder text=new StringBuilder(); Block(int id,String l){speakerId=id;label=l;} }
    private final List<RawSegment> raw=new ArrayList<>(); private long lastAcceptedMidSample=-1L;

    public synchronized void acceptWindow(WhisperBridge.Result result,long windowStartSample,SpeakerDiarizationEngine diarizer){
        if(result==null||result.segments==null)return;
        for(WhisperBridge.Segment s:result.segments){String text=s.text==null?"":s.text.trim();if(text.isEmpty())continue;
            long start=windowStartSample+Math.max(0L,s.t0)*SAMPLES_PER_WHISPER_TICK;
            long end=windowStartSample+Math.max(s.t0,s.t1)*SAMPLES_PER_WHISPER_TICK;if(end<=start)end=start+1;long mid=start+(end-start)/2;
            if(mid<=lastAcceptedMidSample)continue;raw.add(new RawSegment(start,end,text));lastAcceptedMidSample=mid;
        }
    }

    public synchronized String text(SpeakerDiarizationEngine diarizer){
        List<Block> blocks=buildBlocks(diarizer);StringBuilder out=new StringBuilder();
        for(Block b:blocks){if(out.length()>0)out.append('\n').append('\n');out.append(b.label).append(": ").append(b.text.toString().trim());}
        return out.toString().trim();
    }
    public synchronized String plainText(){StringBuilder out=new StringBuilder();for(RawSegment s:raw){if(out.length()>0)out.append(' ');out.append(s.text);}return out.toString().replaceAll("\\s+"," ").trim();}

    private List<Block> buildBlocks(SpeakerDiarizationEngine diarizer){
        ArrayList<Block> blocks=new ArrayList<>();
        for(RawSegment s:raw){String[] words=s.text.trim().split("\\s+");if(words.length==0)continue;long duration=Math.max(1,s.end-s.start);
            for(int i=0;i<words.length;i++){long a=s.start+(duration*i)/words.length;long b=s.start+(duration*(i+1))/words.length;long mid=a+(b-a)/2;
                int speaker=diarizer==null?0:diarizer.speakerAtSample(mid);String label=diarizer==null?"Sprecher ?":diarizer.labelForSpeaker(speaker);
                Block block=blocks.isEmpty()?null:blocks.get(blocks.size()-1);if(block==null||block.speakerId!=speaker||!block.label.equals(label)){block=new Block(speaker,label);blocks.add(block);}
                if(block.text.length()>0)block.text.append(' ');block.text.append(words[i]);
            }
        }
        return blocks;
    }
}
'''
Path('SpeechNotes/app/src/main/java/com/chatgpt/speechnotes/SpeakerTranscriptFusion.java').write_text(fusion)

# Version bump after v2.0.1 migration patch.
p = Path('SpeechNotes/app/build.gradle')
s = p.read_text()
if not re.search(r'versionCode\s+21', s) or not re.search(r"versionName\s+['\"]2\.0\.1['\"]", s):
    raise SystemExit('v2.1 expected effective v2.0.1 build state')
s = re.sub(r'versionCode\s+21', 'versionCode 22', s, count=1)
s = re.sub(r"versionName\s+['\"]2\.0\.1['\"]", "versionName '2.1.0'", s, count=1)
p.write_text(s)

print('Applied Speech Notes v2.1.0 speaker identity + diarization v2')
