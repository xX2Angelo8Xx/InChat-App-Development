# Speech Notes Architecture

## Streaming path

```text
Microphone → AudioRecord → 16 kHz mono PCM16 → 90 s ring buffer
→ 25 s windows / 3 s overlap / 22 s stride
→ single Whisper worker → overlap deduplication
→ live + committed transcript
```

Capture and inference are intentionally decoupled. Runtime/session state has one authority, inference uses a bounded queue, and stop semantics include a mandatory final flush.

## Speaker diarization

```text
Audio
├── Whisper → what was said
└── VAD → CAM++ embedding → online speaker clustering/tracking
    → final session-wide refinement → timestamp fusion
```

The speaker subsystem uses a generic `List<SpeakerCluster>` representation with a current policy limit of three anonymous speakers. Diarization does not perform source separation and therefore does not itself solve overlapping-speech recognition.

## Productive baseline
Large-v3-Turbo Q4_0, stock ARMv9, 4 threads, `audio_ctx=1280`, German, CPU-only, Flash Attention disabled.
