# Speech Notes Project History

Speech Notes is the first major application developed under InChatAppDevelopment. Early work was bootstrapped in the unrelated `xX2Angelo8Xx/Drone-Fieldtest` repository on an app-specific branch. The placement was historical convenience, not an architectural relationship.

On 2026-08-22, the application was extracted from immutable source commit `f280ba1e6fa8d50ab4af863642c2eaf9a3dec231` into `InChat-App-Development/apps/speech-notes` without modifying `Drone-Fieldtest/master`.

Important engineering evolution includes CPU/backend benchmarking, Q4_0 quantization, runtime-session self-healing, continuous streaming with bounded inference, diagnostics, and a max-three-speaker CAM++/ONNX Runtime diarization extension.
