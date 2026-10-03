# Build and release

Toolchain: JDK 17, Gradle 8.13, Android Gradle Plugin 8.13.0, Kotlin 2.1.20, SDK/API 36 and build tools 36.0.0. Maven dependencies are fixed versions.

From the repository root:

```sh
bash tooling/local-media/prepare-engine.sh
base64 --decode tooling/local-media/signing/playtest.jks.b64 > apps/local-media/android/playtest.jks
cd apps/local-media/android
gradle :app:assembleRelease :app:testReleaseUnitTest :app:lintRelease
gradle :app:connectedDebugAndroidTest
```

Workflow `.github/workflows/local-media.yml` builds only this app on `feat/local-media`, checks unit tests/lint, runs offline native runtime tests on an Android 16 x86_64 emulator, inspects package/version/signature/native ABI contents/pinned engine/EJS, then publishes `localmedia-v0.1.0` / `LocalMedia-0.1.0.apk` as a prerelease. Final physical ARM64 phone acceptance remains required.

A fixed app-specific public playtest keystore is used for reproducible test updates, following the repository's test-build convention. It is intentionally not a production signing identity; do not publish this signing key as a trusted production release. Production distribution requires a separate secure signing decision. Its package identity is new, so no installed app is replaced. For test updates preserve signing and increment versionCode.

The source commit is pinned by the release tag. Bundled yt-dlp is fetched from an immutable official release with SHA-256 verification. Runtime updates require a new validated APK release; no hidden automatic update is performed.
