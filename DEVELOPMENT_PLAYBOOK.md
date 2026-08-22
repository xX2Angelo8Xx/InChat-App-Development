# InChatAppDevelopment – Development Playbook

## Standard workflow

```text
User goal
  ↓
Inspect repository/current system
  ↓
Define target architecture
  ↓
Implement directly
  ↓
Reproducible CI
  ↓
Inspect exact failure state/logs
  ↓
Fix root cause
  ↓
Compile + lint/tests
  ↓
Inspect artifact + signing
  ↓
Release
  ↓
Physical/device validation
  ↓
Measured feedback
```

## New application workflow
Before implementation: allocate `apps/<app>`, document architecture/build/release conventions, create an app-scoped CI workflow with path filters, and ensure no coupling to unrelated app directories.

## Source of truth
- Code: this GitHub repository.
- Cross-project process: root project documentation.
- App architecture: documentation inside the app directory.
- Runtime truth: measured telemetry, logs, benchmarks and device behavior.
- Release truth: completed CI/release state plus published artifact.

## Failure handling
Inspect the failing job and exact step; identify the first real error; distinguish root cause from secondary warnings; fix the root cause; rerun without weakening validation gates.

## Performance engineering
Use controlled A/B comparisons with the same input, device, model, runtime state and measurement path. Preserve a validated productive baseline while adding architecture around it.

## Runtime systems
Capture/input must not be blocked by slow inference; queues must be bounded; backpressure must be explicit; native resources require clear ownership; backlog and final-flush semantics must be measurable.

## Completion standard
- Code-only refactor: compile/lint passes.
- Release candidate: compile, lint/tests, packaging, artifact inspection and release publication.
- Runtime-sensitive mobile capability: release publication plus physical device validation.
