# Speech Notes Device Validation

Migration/release validation on the Samsung Galaxy A56 5G should cover: clean/cold start; model load; recording start/stop; streaming transcript progression; mandatory final flush; speaker diarization; repeated sessions; background/display interruption behavior; long-run queue/backlog stability; memory/thermal behavior; productive runtime performance; conversational transcription quality; and upgrade installation over an existing signed Speech Notes build.

## v2.1.0 Speaker Identity & Diarization v2

Validate the following separately so accuracy and performance regressions can be isolated:

1. **Profile enrollment:** create a profile from the guided 50 s / two-page text, verify the name prompt, profile persistence after app restart, displayed quality/prototype count, and complete profile deletion.
2. **Known single speaker:** one enrolled person speaks for several minutes; the transcript should use the saved name rather than `Sprecher 1` for confidently matched clusters.
3. **Known + unknown:** one enrolled and one non-enrolled person alternate; the known person should receive the saved name while the unknown person remains `Sprecher N` rather than receiving a forced identity.
4. **Multiple known speakers:** enroll at least three people and verify stable names across repeated turns and a complete session.
5. **Fast turns:** alternate speakers every 1–3 seconds and verify that temporal smoothing does not suppress real sustained changes or create isolated speaker flicker.
6. **Mid-sentence turn:** change speaker without a long pause inside one grammatical sentence; verify that v2.1 fusion can split the text inside a Whisper segment instead of assigning the complete segment to one speaker.
7. **Participant limit:** exercise configured limits 1, 2, 3, 5 and 10. The actual cluster count must remain `<= configuredMaxSpeakers`; the configured number must never force creation of unused clusters.
8. **Similar voices / ambiguity:** test acoustically similar speakers and ensure uncertain profile matches stay anonymous rather than being assigned a wrong saved name.
9. **Long session:** 20–30 minute conversation with repeated speaker returns; inspect cluster fragmentation, duplicate identity assignment, queue/backlog, CAM++ embedding time, memory and thermal behavior.
10. **Upgrade path:** install v2.1.0 over the prior signed release and verify application data/history remains available and the package/signing identity is accepted by Android.

Record for each test: configured max speakers, enrolled profile set, actual people present, transcript output, incorrect speaker boundaries, incorrect identity matches, speaker embedding runs/wall time, Whisper performance, queue/drop telemetry, device temperature/thermal behavior and any fallback/error diagnostics.

Migration parity and v2.1.0 runtime acceptance are complete only after a new-repository release installs and behaves correctly on the physical target device.
