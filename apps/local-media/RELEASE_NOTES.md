Local Media 0.1.1 — installer compatibility trial

Adds v1/JAR signing alongside v2 with the existing playtest certificate. Tests the final release APK through Android PackageManager (name, icon, version and both signature APIs), in addition to ADB install, offline runtime, lint, unit tests and package inspection.

The 0.1.0 APK fails before install confirmation on Galaxy A56 / Android 16 despite matching the published file exactly. This release tests one signing change; the root cause is not established and phone installation is not yet confirmed. SDK level, native ABIs, bundled engines and app behavior remain unchanged.

Standalone local MP3/MP4 downloader. Android 10+, ARM64 and x86_64. Public playtest key; physical device and live YouTube acceptance pending.
