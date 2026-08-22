# Speech Notes Project History

Speech Notes is the first major application developed under InChatAppDevelopment. Early work was bootstrapped in the unrelated `xX2Angelo8Xx/Drone-Fieldtest` repository on an app-specific branch. The placement was historical convenience, not an architectural relationship.

On 2026-08-22, the application was extracted from immutable source commit `f280ba1e6fa8d50ab4af863642c2eaf9a3dec231` into `InChat-App-Development/apps/speech-notes` without modifying `Drone-Fieldtest/master`.

Important engineering evolution includes CPU/backend benchmarking, Q4_0 quantization, runtime-session self-healing, continuous streaming with bounded inference, diagnostics, and CAM++/ONNX Runtime diarization.

## v2.1.0 — Speaker Identity & Diarization v2

v2.1.0 replaces the fixed max-three-speaker product policy with a user-selectable 1–10 participant limit and adds persistent, fully local speaker identities. A guided approximately 50-second enrollment captures voice data only in memory, derives multiple CAM++ embeddings, filters weak/inconsistent samples, stores a robust profile centroid/prototype set, asks for a display name, and discards the raw enrollment audio.

The diarization timeline is upgraded from coarse speech chunks to overlapping 1.5-second CAM++ observations at a 0.5-second hop, followed by temporal sequence smoothing and final global centroid refinement. Saved identities are matched conservatively as an open-set recognition problem; uncertain clusters remain anonymous `Sprecher N` rather than receiving a forced name.

Transcript fusion no longer assigns an entire Whisper segment to only one speaker. v2.1.0 interpolates word positions across each timestamped Whisper segment and resolves every word against the finer speaker-turn timeline, allowing speaker changes inside a Whisper segment while retaining the validated Whisper segment-timestamp inference path.
