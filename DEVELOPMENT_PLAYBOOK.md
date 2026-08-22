InChat App Development — Development Playbook

Standard Workflow

User goal
   ↓
Read AGENTS.md + canonical rules
   ↓
Inspect affected app and repository state
   ↓
Identify current architecture / CI / version / release state
   ↓
Define target architecture when needed
   ↓
Implement directly
   ↓
Run reproducible CI
   ↓
Inspect exact CI result and logs
   ↓
Fix root cause
   ↓
Build / lint / tests
   ↓
Inspect produced artifact
   ↓
Publish release when requested/appropriate
   ↓
Physical device or user validation
   ↓
Measured feedback
   ↓
Next iteration

Before Implementation

For a new app, migration, or major subsystem:

1. identify the target app directory;


2. inspect its source layout;


3. inspect build tooling;


4. inspect CI workflows;


5. inspect versioning and signing;


6. inspect release conventions;


7. identify protected/default branches;


8. distinguish technical debt from the requested feature;


9. define the target architecture before large changes.



Small contained bug fixes may be implemented directly.

Architecture Rules

For large or runtime-sensitive changes, define:

data flow;

ownership of runtime state;

thread/process boundaries;

lifecycle behavior;

persistence;

failure containment;

performance-sensitive paths;

telemetry;

compatibility implications;

release implications.


Prefer generic structures when future scale is already foreseeable. Avoid hardcoding architecture around today's exact count or temporary experiment.

Implementation Style

Prefer:

deterministic changes;

readable source;

explicit state ownership;

bounded queues;

safe concurrency;

fail-fast validation;

reproducible dependencies;

diagnostics around expensive/asynchronous operations;

modular components;

safe fallbacks where appropriate.


Avoid:

hidden side effects;

duplicate sources of truth;

uncontrolled parallel access to native resources;

speculative fixes without logs;

silent patch failure;

build-time mutation that cannot be reproduced;

committing CI status noise into the normal source history unless there is a strong reason.


CI Is Part of the Implementation

A feature is not complete merely because source code looks correct.

Useful CI phases include:

initialize
toolchains
dependencies
models/assets
native_build
app_build
tests
lint/static_analysis
signing
artifact_inspection
artifact_upload
release
release_verification

CI Failure Handling

When CI fails:

1. identify the exact run;


2. inspect the failing job;


3. inspect the first failing step;


4. fetch full logs when needed;


5. identify the first real error;


6. separate root cause from downstream errors;


7. verify assumptions against actual repository state;


8. fix the root cause;


9. rerun;


10. retain all validation gates.



Do not repeatedly modify assertions without first deriving the actual state they are intended to validate.

Versioning

Use semantic intent:

patch (x.y.Z): bug fix, migration correction, compatibility correction, diagnostics, small behavior correction;

minor (x.Y.0): meaningful feature or architectural extension;

major (X.0.0): substantial product capability or intentionally breaking milestone.


For Android:

versionName communicates product version;

versionCode must monotonically increase for every installable update.


Do not infer the next versionCode. Read the actual effective build state first.

Release Discipline

Before publishing or sharing a release link:

confirm the workflow succeeded;

confirm the artifact exists;

confirm the intended version is inside the artifact;

confirm signing passed;

confirm package/application identity is expected;

confirm required assets/native libraries are present;

confirm prohibited components are absent when relevant;

confirm the GitHub Release asset exists.


A failed or cancelled workflow is not a release.

Performance Engineering

Use controlled comparisons:

A vs B
same device
same input
same model/data
same runtime state
same measurement path
multiple repetitions

Track relevant metrics such as:

wall time;

real-time factor;

latency;

memory/RSS;

CPU behavior;

thermal state;

queue backlog;

dropped work;

model load time;

native timing breakdown;

quality/accuracy.


Once a productive baseline is validated, preserve it while experimenting around it.

Real-Time / Native Systems

For mobile real-time pipelines:

input/capture must not block on slow inference;

bounded queues and backpressure policy must be explicit;

shared native resources must have clear ownership;

final-flush semantics must be explicit;

lifecycle transitions must be observable;

diagnostics should exist at worker/resource boundaries;

runtime recovery must not create duplicate model/session truth.


Device Testing

Release candidates should use an app-specific checklist. Typical checks:

cold start;

warm start;

repeated use;

permissions;

state transitions;

background/foreground behavior;

interruption handling;

long-run stability;

memory;

thermal behavior;

performance;

quality/accuracy;

failure diagnostics;

upgrade from the prior released version when update compatibility matters.


Decision Escalation

Proceed autonomously when a decision is reversible, technically determined, and covered by existing policy.

Pause and ask the user when a decision:

changes product behavior materially;

changes a public/package identity;

changes signing identity;

deletes historical data;

rewrites Git history;

modifies a protected/unrelated repository;

introduces a meaningful privacy/security tradeoff;

is not technically resolvable from current requirements.