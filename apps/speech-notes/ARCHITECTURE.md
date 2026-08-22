# Speech Notes Architecture

## Streaming path

```text
Microphone → AudioRecord → 16 kHz mono PCM16 → 90 s ring buffer
→ 25 s windows / 3 s overlap / 22 s stride
→ single Whisper worker → overlap deduplication
→ live + committed transcript
```

Capture and inference are intentionally decoupled. Runtime/session state has one authority, inference uses a bounded queue, and stop semantics include a mandatory final flush.

## Speaker diarization v2

```text
Audio
├── Whisper → timestamped segments → word-time interpolation
└── adaptive VAD → 1.5 s CAM++ windows / 0.5 s hop
    → dynamic online clustering (configured 1..10 speakers)
    → temporal sequence smoothing
    → final session-wide centroid refinement
    → conservative local voice-profile matching
    → speaker-turn timeline

Whisper word-time interpolation + speaker-turn timeline
    → mid-segment speaker-aware transcript fusion
```

The session representation remains generic `List<SpeakerCluster>`, but the maximum cluster count is now a user-configurable policy from one to ten participants. Sliding CAM++ observations replace the earlier coarse up-to-3.5-second chunks so speaker changes can be localized at substantially finer time resolution. A sequence smoother adds a switching penalty to reject isolated classification flips while still allowing sustained speaker changes.

Whisper remains on the validated segment-timestamp path in v2.1.0. When a speaker boundary occurs inside one Whisper segment, the fusion layer distributes words monotonically across the segment duration and assigns each interpolated word position to the diarization timeline. This removes the previous one-speaker-per-Whisper-segment limitation without enabling experimental Whisper token timestamps.

## Speaker identity

Speaker profiles are fully local. Guided enrollment records approximately 50 seconds of speech in memory, computes multiple CAM++ embeddings, rejects weak/inconsistent samples, stores a normalized centroid plus a bounded prototype set, and discards the raw enrollment audio. The persisted profile contains a UUID, display name, creation time, embedding data, prototype count and consistency score.

Identity matching is open-set and conservative: a session cluster receives a saved display name only when the best profile similarity exceeds the configured threshold and has sufficient separation from the next-best profile. Otherwise the transcript keeps the anonymous `Sprecher N` label. Diarization and identity remain separate concepts: clustering determines who is acoustically consistent within a session; profile matching optionally maps such a cluster to a known local identity.

Diarization does not perform source separation and therefore does not itself solve truly overlapping simultaneous speech.

## Productive Whisper baseline
Large-v3-Turbo Q4_0, stock ARMv9, 4 threads, `audio_ctx=1280`, German, CPU-only, Flash Attention disabled.
