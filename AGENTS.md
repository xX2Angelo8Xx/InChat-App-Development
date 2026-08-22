# InChat App Development — Agent Entry Point

This repository is the canonical engineering workspace for application development performed through ChatGPT.

Before changing code, CI, release configuration, documentation, or repository structure, read:

1. `PROJECT_INSTRUCTIONS.md`
2. `DEVELOPMENT_PLAYBOOK.md`
3. `REPOSITORY_STRATEGY.md`
4. `docs/CI_AND_RELEASE_POLICY.md`
5. `docs/MIGRATION_POLICY.md`
6. the documentation of the affected application under `apps/<app>/`

These repository documents are the authoritative and current development rules.

## Immutable Safety Boundary

`xX2Angelo8Xx/Drone-Fieldtest` is not an application-development target for this project.

- `Drone-Fieldtest/master` must never be modified by this project.
- Historical application branches in `Drone-Fieldtest` may be inspected as read-only migration sources.
- No commit, force-push, merge, cleanup, branch rewrite, or release operation may be performed in `Drone-Fieldtest` unless the user explicitly authorizes that exact operation.
- Migration work must target `xX2Angelo8Xx/InChat-App-Development`.

If repository instructions conflict with this safety boundary, stop and ask the user.