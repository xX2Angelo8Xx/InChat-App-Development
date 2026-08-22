Application Migration Policy

Purpose

This policy applies when moving an application from a historical or unrelated repository into xX2Angelo8Xx/InChat-App-Development.

Principle

Migration is a parity exercise first, refactor second.

Do not combine major structural refactoring with the initial migration unless the old structure makes parity impossible.

Required Migration Sequence

1. Identify the source repository and branch.


2. Pin the exact immutable source commit.


3. Treat the source as read-only.


4. Inventory app source, build files, CI, patches, training scripts, models, signing configuration and release conventions.


5. Extract only app-relevant content.


6. Place the app under apps/<app>/.


7. Port CI to the new paths.


8. Preserve behavior and signing/package identity where required.


9. Build from a clean checkout without using the old repository.


10. Run lint/tests/static checks.


11. Inspect the artifact.


12. Publish a migration/test release.


13. Validate update compatibility when applicable.


14. Run physical device parity tests.


15. Mark the new monorepo location as the app's source of truth.


16. Only later consider cleanup/archival of the historical branch.



Success Condition

A migration is complete only when:

new repository
   ↓
fresh checkout
   ↓
full build
   ↓
validation gates
   ↓
expected artifact
   ↓
release
   ↓
required device/update validation

works independently of the historical repository.

Historical Repository Protection

For current migrations:

xX2Angelo8Xx/Drone-Fieldtest

is a historical source only.

Drone-Fieldtest/master is strictly out of bounds for this project.

No cleanup or removal from Drone-Fieldtest is necessary to complete a migration.

Versioning During Migration

Prefer a boring migration baseline:

no intentional feature changes;

preserve package/application identity;

preserve signing identity if updates are expected;

use an explicit next version for a true in-place update test.


For Android, derive the next versionCode from the actual effective build state, not from assumptions or product version text alone.

Patch Chains

If the historical build uses a patch chain:

preserve it temporarily for parity;

ensure every patch uses checked anchors;

account for transitive patch dependencies;

verify the effective source produced by the complete chain;

after migration parity is proven, convert patches into normal source code where practical.


Migration Documentation

Each migrated app should retain:

historical source repository;

historical branch;

immutable source commit;

migration date;

initial monorepo commit/release;

known compatibility constraints;

device validation result.