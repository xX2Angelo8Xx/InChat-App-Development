# Speech Notes Benchmarks

Validated target-device findings:

- Large-v3-Turbo Q4_0 substantially outperformed Q5_0 on the Samsung Galaxy A56 5G.
- Productive Q4_0 processing reached roughly 11–12 s for a 25 s audio window (approx. RTF 0.45–0.5).
- Stock ARMv9 outperformed the tested KleidiAI productive configuration.
- Four threads were preferable to higher thread counts in productive tests.
- `audio_ctx=1280` is the current quality/performance baseline; smaller contexts sometimes caused quality degradation or hallucination.
- Flash Attention caused severe slowdown and remains disabled.

These findings are device/application specific and must not be generalized without controlled testing.
