CI and Release Policy

Objective

CI must make the state of every app build reproducible, inspectable and externally verifiable.

Required Properties

Each releasable app should have CI that:

checks out the intended source revision;

pins or deterministically resolves important toolchains/dependencies;

builds from a clean environment;

runs relevant tests/static checks;

signs release artifacts when applicable;

inspects the produced artifact;

uploads an artifact;

publishes a release when the workflow is a release workflow;

allows the final state to be inspected after the run.


Hard Gates

Do not bypass these merely to get a green build:

compilation;

tests where present/relevant;

lint/static analysis;

signing verification;

package/version verification;

artifact-content inspection;

release-asset verification.


If a gate is wrong, correct the gate based on measured/derived state rather than disabling it.

Android Release Verification

Before a normal Android release is considered complete, verify as applicable:

package/application ID;

versionName;

monotonically increasing versionCode;

signing certificate/identity;

ABI contents;

required native libraries;

required models/assets;

prohibited libraries/features are absent;

APK/AAB can be parsed by Android build tools;

GitHub Release contains the expected named asset.


Download Links

Only provide a user-facing APK/release download link after:

1. the relevant workflow concluded successfully;


2. the expected release exists;


3. the expected asset exists in that release.



Do not provide guessed or future release URLs.

CI Telemetry

Prefer GitHub Actions/release state as the primary telemetry source.

If auxiliary telemetry is stored in the repository:

it must not create build loops;

it should not pollute normal source history unnecessarily;

it must not be treated as more authoritative than the actual workflow run.


Failure Diagnosis

Use exact logs.

The diagnostic order is:

1. run conclusion;


2. failing job;


3. failing step;


4. first actual error;


5. effective source/build state at that step.



Do not infer generated/patch-mutated file contents from the pre-build repository when the workflow itself changes those files.

Release Naming

Use stable product-oriented tags and asset names, for example:

speechnotes-v2.0.1
SpeechNotes-2.0.1.apk

Avoid using ambiguous names such as latest.apk as the only released artifact.

Physical Validation

CI success proves build/release integrity, not runtime quality.

Runtime-sensitive releases remain pending full acceptance until required physical-device tests are complete.