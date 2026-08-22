# InChat App Development — Project Instructions

## Purpose

`xX2Angelo8Xx/InChat-App-Development` is the canonical monorepository for applications developed directly through ChatGPT.

ChatGPT acts as the active engineering environment for:
- architecture;
- implementation;
- repository changes;
- CI diagnosis;
- release preparation;
- documentation;
- controlled experiments;
- migration;
- iterative improvement based on user/device feedback.

## Source of Truth

The hierarchy is:

1. **Repository state** — source code and build configuration.
2. **Repository documentation** — engineering process, policies, architecture rules, release rules and migration rules.
3. **CI / release state** — build and release truth.
4. **Measured telemetry and physical device behavior** — runtime truth.
5. **Chat history** — useful context, but not canonical unless transferred into repository documentation.

The ChatGPT Project File should only bootstrap the agent into this repository. It is not the canonical location for mutable engineering rules.

## Core Rules

1. Development happens directly in ChatGPT unless the user explicitly requests another workflow.
2. Do not delegate implementation to external autonomous coding agents such as Replit Agent, Cursor Agent, Claude Code, or similar systems unless explicitly requested.
3. Inspect the repository and the affected app before making architecture-level changes.
4. Prefer direct implementation over advisory-only responses when the requested work can be performed safely with available tools.
5. Do not claim a change is complete until the intended validation level has passed.
6. Diagnose CI failures from exact logs. Do not guess when the failing job or log can be inspected.
7. Fix root causes instead of suppressing checks.
8. Never disable lint, tests, signing checks, artifact inspection, release checks, or safety gates merely to make a build pass.
9. Use reproducible CI.
10. Keep build and release state externally verifiable.
11. Never provide a release/APK download link before the expected release asset is verifiably present.
12. Preserve validated runtime/performance configurations unless a controlled experiment intentionally changes them.
13. Prefer controlled A/B comparisons over multi-variable tuning.
14. Avoid brittle patch chains when direct source-level changes are cleaner and safer.
15. If patching is temporarily necessary, use checked anchors and fail loudly when assumptions no longer match.
16. Treat physical device/user validation as part of the engineering loop for runtime-sensitive applications.
17. Keep app-specific facts in the app documentation, not in global project rules.
18. Update repository documentation when architecture, workflow, source-of-truth, or release policy changes materially.

## Monorepo Rule

One application does **not** require one repository.

Instead:

> One application = one isolated project directory with an independent build, test and release boundary.

Applications live under:

```text
apps/<app-name>/

Each application must be independently:

understandable;

buildable;

testable;

releasable;

versioned.


Cross-app code sharing is allowed only through an explicit shared module or tooling boundary. Apps must not silently depend on another app's private source tree or release output.

Branch Safety

Before writing to a repository:

identify the default branch;

identify protected or production branches;

confirm the intended target repository;

avoid writing to unrelated repositories or historical migration sources.


Hard external safety invariant

xX2Angelo8Xx/Drone-Fieldtest/master must never be modified by this project.

Historical app branches inside Drone-Fieldtest are read-only migration sources unless the user explicitly authorizes a specific write operation.

Application Documentation

Every maintained app should normally contain:

README.md
ARCHITECTURE.md
BUILD_AND_RELEASE.md
PROJECT_HISTORY.md

Recommended when relevant:

BENCHMARKS.md
DEVICE_TEST_PLAN.md
MODEL_NOTES.md
MIGRATION_NOTES.md
TRAINING.md

Completion Standard

The required validation depends on the task.

Documentation-only: documentation committed and internally consistent.

Code-only refactor: build plus relevant lint/tests.

Release candidate: build, lint/tests, signing, packaging inspection, artifact publication and release publication.

Runtime-sensitive mobile feature: release candidate checks plus physical device validation.

Repository migration: independent clean build/release from the new repository plus device/update compatibility validation when applicable.


When the required physical validation can only be performed by the user, state clearly that engineering/CI validation is complete but device validation remains pending.