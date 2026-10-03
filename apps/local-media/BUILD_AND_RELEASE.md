# Build and release

Toolchain: JDK 17, Gradle 8.13, Android Gradle Plugin 8.13.0, Kotlin 2.1.20, SDK/API 36 and build tools 36.0.0. Maven dependencies are fixed versions.

From the repository root:

```sh
bash tooling/local-media/prepare-engine.sh
base64 --decode tooling/local-media/signing/playtest.jks.b64 > apps/local-media/android/playtest.jks
cd apps/local-media/android
gradle :app:assembleRelease :app:testReleaseUnitTest :app:lintRelease
adb push app/build/outputs/apk/release/app-release.apk /data/local/tmp/local-media-release.apk
gradle :app:connectedDebugAndroidTest
```

Workflow `.github/workflows/local-media.yml` builds only this app on `feat/local-media`, checks unit tests/lint, runs offline native runtime tests on an Android 16 x86_64 emulator, inspects package/version/signature/native ABI contents/pinned engine/EJS, then publishes `localmedia-v0.2.0` / `LocalMedia-0.2.0.apk` as a prerelease. Final physical ARM64 phone acceptance remains required.

A fixed app-specific public playtest keystore is used for reproducible test updates, following the repository's test-build convention. It is intentionally not a production signing identity; do not publish this signing key as a trusted production release. Production distribution requires a separate secure signing decision. Its package identity is new, so no installed app is replaced. For test updates preserve signing and increment versionCode.

The source commit is pinned by the release tag. Bundled yt-dlp is fetched from an immutable official release with SHA-256 verification. Runtime updates require a new validated APK release; no hidden automatic update is performed.

## Installer compatibility trial (0.1.1)

The user reports an immediate pre-install parsing error (app shown as Unknown) on Galaxy A56 / Android 16, including when opening from a new internal-storage folder. Their uploaded 0.1.0 APK is byte-identical to the release (113080966 bytes; SHA-256 ef4fef1aa02d440169373225af8da53d535434b2c08f08c2a14078a0c87bc012). ZIP CRC, manifest, v2 signature, 4-byte ZIP alignment, uncompressed resources.arsc and ARM64 executable 16-KB ELF alignment checks passed. ADB release install succeeded in the original Android 16 x86_64 test. These checks do not reproduce Samsung's installation path.

0.1.1 is a controlled trial: explicitly add JAR/v1 signing alongside v2 using the same certificate. Keep SDK/ABI/runtime/resource packaging settings unchanged. The cause is not established; do not describe v1 as required by Android 16 or this release as a proven phone fix. CI now requires both schemes, checks zipalign and ZIP CRC/resource storage, and reads the actual release archive via PackageManager with modern and legacy signature flags, loading its name/icon and checking its certificate. Phone installation acceptance remains pending; if this trial fails, obtain installer diagnostics before broader package changes.

## 0.2.0 preview and download pipeline

VersionCode 3 retains the same package/certificate and both signing schemes. Installation/start was reported working after a phone reboot; no cause was established for the former installer parsing failure. A subsequent real audio request failed in the yt-dlp ExtractAudioPP stage while renaming a missing original WebM. The screenshot does not establish why that input disappeared. The replacement downloads tracks into durable UUID job directories, keeps them intact, and invokes FFmpeg separately before MediaStore publication.

Validation adds fixture-based actual format parsing (protected/unsupported tracks excluded), preview-before-download UI and link invalidation, and an offline end-to-end actual yt-dlp WebM file download → FFmpeg MP3 conversion → MediaStore publication, plus separate H.264 video/audio MP4 muxing. The CI preview artifact includes the populated metadata screen. These tests are independent of live YouTube and do not claim to reproduce all phone/network conditions. Retest the originally failing live request on the physical phone.
