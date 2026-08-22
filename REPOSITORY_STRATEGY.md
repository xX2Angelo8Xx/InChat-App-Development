# InChatAppDevelopment – Repository Strategy

## Objective
This repository is an application monorepo designed to host multiple independent products without cross-contamination.

## Target structure

```text
InChat-App-Development/
├── README.md
├── PROJECT_INSTRUCTIONS.md
├── DEVELOPMENT_PLAYBOOK.md
├── REPOSITORY_STRATEGY.md
├── apps/
│   ├── speech-notes/
│   └── <future-app>/
├── shared/
├── tooling/
└── .github/workflows/
```

## Isolation rule
The governing rule is:

> one application = one isolated project directory

A repository may contain many applications, but every application must be independently buildable, testable and releasable. A change to one app should not trigger unrelated application builds unless an explicitly shared dependency changed.

## CI strategy
Application workflows use path filters. Shared tooling may trigger all affected consumers. Workflows must operate from a fresh checkout and may not depend on files in unrelated repositories.

## Branching
`main` is the default repository branch. Keep it releasable. Feature branches are recommended for changes with meaningful integration risk; small deterministic maintenance can be committed directly when explicitly authorized.

## Historical Drone-Fieldtest boundary
Speech Notes was originally bootstrapped inside `xX2Angelo8Xx/Drone-Fieldtest` on a dedicated app branch. This was a historical mistake and is not the long-term model.

The immutable Speech Notes migration source is:
`f280ba1e6fa8d50ab4af863642c2eaf9a3dec231`.

`Drone-Fieldtest/master` is unrelated production code and must never be modified by InChat application development. The old app branch is retained only as historical evidence until migration parity is validated.

## Signing and secrets
Preserve Android signing identity whenever upgrade compatibility is required. Production signing material should ultimately live in repository-scoped secure CI storage. A migrated legacy key may remain temporarily only to establish binary/update parity and must be treated as technical debt.
