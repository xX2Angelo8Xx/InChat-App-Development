# Speech Notes

Native Android application for fully local/offline speech transcription.

Primary validated target: Samsung Galaxy A56 5G, Android 16, arm64-v8a.

Productive Whisper baseline:

```text
Model: Large-v3-Turbo Q4_0
Backend: stock ARMv9
Threads: 4
audio_ctx: 1280
Language: de
Flash Attention: off
GPU/Vulkan: off
```

The Android project lives in `android/`. The current CI intentionally preserves the legacy patch chain for migration parity; this is transitional technical debt, not the target architecture.
