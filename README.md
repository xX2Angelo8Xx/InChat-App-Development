# InChat App Development

Central monorepository for real applications engineered directly through ChatGPT conversations.

## Repository model

Each application owns an isolated directory under `apps/` and must remain independently buildable, testable and releasable. Shared implementation is allowed only through an explicit reusable module under `shared/` or `tooling/`.

```text
apps/
  speech-notes/
  <future-app>/
tooling/
  speech-notes/
.github/workflows/
```

`xX2Angelo8Xx/Drone-Fieldtest` is unrelated to this repository. It was historically used as a bootstrap location for early app development and is now treated only as a migration archive. InChat application work must never modify `Drone-Fieldtest/master`.

## Current applications

- `apps/speech-notes` — native Android, fully local/offline Whisper transcription with streaming and speaker diarization.

See `PROJECT_INSTRUCTIONS.md`, `DEVELOPMENT_PLAYBOOK.md`, and `REPOSITORY_STRATEGY.md` before starting development.
