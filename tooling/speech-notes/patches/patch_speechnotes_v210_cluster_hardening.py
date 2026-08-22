from pathlib import Path

p = Path('SpeechNotes/app/src/main/java/com/chatgpt/speechnotes/SpeakerDiarizationEngine.java')
s = p.read_text()
old_fields = '''    private long processedUntilSample, embeddingWallMs;\n    private int embeddingRuns;\n'''
new_fields = '''    private long processedUntilSample, embeddingWallMs;\n    private int embeddingRuns;\n    private float[] pendingSpeakerCandidate;\n    private int pendingSpeakerCandidateHits;\n'''
if old_fields not in s:
    raise SystemExit('v2.1 cluster hardening field anchor missing')
s = s.replace(old_fields, new_fields, 1)
old_assign = '''    private Assignment assign(float[] e){\n        if(clusters.isEmpty()){clusters.add(new SpeakerCluster(1,e));return new Assignment(1,1f);}\n        SpeakerCluster best=null;float sim=-1f;\n        for(SpeakerCluster c:clusters){float x=c.similarity(e);if(x>sim){sim=x;best=c;}}\n        if(sim<CREATE_THRESHOLD && clusters.size()<config.maxSpeakers){\n            SpeakerCluster c=new SpeakerCluster(clusters.size()+1,e);clusters.add(c);return new Assignment(c.id(),1f);\n        }\n        if(best==null)return new Assignment(0,0f); best.update(e); return new Assignment(best.id(),sim);\n    }\n'''
new_assign = '''    private Assignment assign(float[] e){\n        if(clusters.isEmpty()){clusters.add(new SpeakerCluster(1,e));return new Assignment(1,1f);}\n        SpeakerCluster best=null;float sim=-1f;\n        for(SpeakerCluster c:clusters){float x=c.similarity(e);if(x>sim){sim=x;best=c;}}\n        if(sim<CREATE_THRESHOLD && clusters.size()<config.maxSpeakers){\n            if(pendingSpeakerCandidate!=null && SpeakerProfile.cosine(pendingSpeakerCandidate,e)>=0.58f){\n                pendingSpeakerCandidateHits++;\n                if(pendingSpeakerCandidateHits>=2){\n                    float[] seed=new float[e.length];\n                    for(int i=0;i<seed.length;i++)seed[i]=(pendingSpeakerCandidate[i]+e[i])*0.5f;\n                    SpeakerCluster.normalize(seed);\n                    SpeakerCluster c=new SpeakerCluster(clusters.size()+1,seed);\n                    clusters.add(c); pendingSpeakerCandidate=null; pendingSpeakerCandidateHits=0;\n                    return new Assignment(c.id(),c.similarity(e));\n                }\n            } else {\n                pendingSpeakerCandidate=e.clone(); pendingSpeakerCandidateHits=1;\n            }\n            // Keep the best known assignment provisionally. Once a second consistent low-similarity\n            // observation confirms a new cluster, sequence smoothing can retroactively relabel it.\n            return best==null?new Assignment(0,0f):new Assignment(best.id(),sim);\n        }\n        pendingSpeakerCandidate=null; pendingSpeakerCandidateHits=0;\n        if(best==null)return new Assignment(0,0f); best.update(e); return new Assignment(best.id(),sim);\n    }\n'''
if old_assign not in s:
    raise SystemExit('v2.1 cluster hardening assign anchor missing')
s = s.replace(old_assign, new_assign, 1)
p.write_text(s)
print('Applied v2.1 repeated-evidence speaker-cluster hardening')
