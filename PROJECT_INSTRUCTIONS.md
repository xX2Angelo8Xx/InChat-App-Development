# InChatAppDevelopment – Project Instructions

## Purpose
InChatAppDevelopment is the engineering framework for building real applications directly inside ChatGPT conversations. ChatGPT acts as the active engineering environment for architecture, implementation, repository changes, CI diagnosis, release preparation and iterative improvement.

## Core rules
1. Development happens directly in ChatGPT unless explicitly overridden.
2. GitHub is the source of truth for code.
3. This repository is the source of truth for cross-application process and conventions.
4. One application = one isolated project directory under `apps/`.
5. Every application must be independently buildable, testable and releasable.
6. Cross-application dependencies require an explicit shared module; accidental path coupling is prohibited.
7. Do not place application code in unrelated repositories.
8. `xX2Angelo8Xx/Drone-Fieldtest/master` is outside the scope of InChat application development and must never be modified by this project.
9. Keep the default branch stable; major work should use dedicated branches when risk warrants it.
10. Use reproducible CI builds and inspect exact logs when failures occur.
11. Never disable lint, tests, signing checks or packaging gates merely to obtain a green build.
12. Preserve validated runtime/performance baselines unless a controlled experiment intentionally changes them.
13. Prefer deterministic source-level implementation over brittle patching. Legacy patch chains may be retained temporarily only for migration parity.
14. Device testing is part of the engineering loop for runtime-sensitive mobile work.
15. Do not claim completion beyond the validation level actually reached.

## Application contract
Each app should normally contain:

```text
apps/<app>/
  README.md
  ARCHITECTURE.md
  BUILD_AND_RELEASE.md
  PROJECT_HISTORY.md
  <platform source>/
```

Optional documents include `BENCHMARKS.md`, `DEVICE_TEST_PLAN.md`, `MODEL_NOTES.md` and `MIGRATION_NOTES.md`.

## Release principle
For Android projects the expected path is source → clean CI checkout → dependency/model preparation → native/managed build → lint/static analysis → signing → APK/AAB inspection → artifact publication → GitHub Release → physical device validation.
