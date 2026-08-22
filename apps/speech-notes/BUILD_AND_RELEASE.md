# Speech Notes Build and Release

The authoritative pipeline is `.github/workflows/speech-notes.yml`.

A clean CI run performs: Android SDK/NDK/CMake setup; pinned whisper.cpp v1.9.1 fetch; legacy migration-parity patches; host quantizer build; model download and checksum verification; Q4_0 generation; signing-key restoration; Gradle/CMake release build; Android lint hard gate; APK/signature/content inspection; artifact upload; GitHub Release publication.

No build is considered releasable if lint, signing, APK inspection or release publication failed.

The current repository-local signing material is retained only to preserve update compatibility with historically published APKs. It should be migrated to secure repository-scoped CI storage after the migration baseline has been validated.
