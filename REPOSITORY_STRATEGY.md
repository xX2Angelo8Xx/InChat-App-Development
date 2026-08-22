InChat App Development — Repository Strategy

Canonical Repository

All active application development for this project belongs in:

xX2Angelo8Xx/InChat-App-Development

The repository is a multi-app monorepo.

Target Structure

InChat-App-Development/
├── AGENTS.md
├── PROJECT_INSTRUCTIONS.md
├── DEVELOPMENT_PLAYBOOK.md
├── REPOSITORY_STRATEGY.md
├── docs/
│   ├── CI_AND_RELEASE_POLICY.md
│   └── MIGRATION_POLICY.md
├── apps/
│   ├── speech-notes/
│   │   ├── README.md
│   │   ├── ARCHITECTURE.md
│   │   ├── BUILD_AND_RELEASE.md
│   │   ├── PROJECT_HISTORY.md
│   │   └── android/
│   ├── digitmatrix/
│   │   └── ...
│   └── future-app/
├── tooling/
│   ├── shared/
│   └── <app-specific-tooling>/
└── .github/
    └── workflows/

Isolation Principle

> One application = one isolated project directory and one independent build/test/release boundary.



An app may use shared tooling, but:

app A must not depend on private implementation details of app B;

release outputs must not be used as hidden build dependencies;

shared source must live in an explicit shared module;

CI should be path-selective where practical.


App Directory Naming

Use stable, descriptive, lowercase kebab-case directory names:

apps/speech-notes/
apps/digitmatrix/
apps/gesture-vision/

Product-facing names may use different capitalization.

CI Organization

Prefer one workflow per independently releasable app:

.github/workflows/speech-notes.yml
.github/workflows/digitmatrix.yml

Use path filters so changes to one app do not rebuild unrelated apps unless shared infrastructure changed.

Shared tooling changes may intentionally trigger multiple workflows.

Migration Sources

Historical app code may exist in unrelated repositories.

Current known historical source:

xX2Angelo8Xx/Drone-Fieldtest

Rules:

migration sources are read-only;

pin the exact source commit;

extract only app-relevant source/configuration;

do not import unrelated repository history/content;

preserve signing identity when update compatibility is required;

prove independent build/release parity before declaring migration complete.


Drone-Fieldtest Safety Boundary

Drone-Fieldtest/master must never be modified by this project.

Known historical app branches may be inspected for migration, but are not active development targets.

The existence of app branches in Drone-Fieldtest does not make that repository part of the new monorepo workflow.

Source of Truth Transition

An app becomes authoritative in InChat-App-Development only after:

1. source migration is complete;


2. CI works from a clean checkout;


3. expected artifact is generated;


4. release publication succeeds when applicable;


5. signing/package identity is verified where applicable;


6. device/update validation succeeds when required.



Until then, clearly label the migration state.

History Strategy

Do not blindly transplant the full Git history of an unrelated repository.

Prefer:

recording the immutable source commit;

preserving relevant historical documentation;

preserving app-specific changelog/project history;

starting clean monorepo history for the migrated app.


Secrets and Signing

Production signing material and secrets should use secure repository-scoped CI storage where supported.

Do not casually duplicate secret material.

When an Android app must update an existing installation:

preserve applicationId;

preserve compatible signing identity;

increment versionCode;

verify the update path on a physical device.


Default Branch

The default branch of InChat-App-Development may be used as the active integration branch when that is the established repository workflow.

Before any destructive or history-rewriting operation, stop and obtain explicit user approval.