# Local Media

A standalone Android app for downloading individual YouTube videos as MP3 or MP4 directly on the device. No backend, account, analytics, conversion service, Termux or separate runtime installation.

- Paste an HTTPS YouTube video link or share one from YouTube.
- MP3: actual local FFmpeg conversion to 192 kbit/s.
- MP4: H.264 video with audio, up to 360p, 720p or 1080p; available resolution may be lower.
- Foreground download with progress and cancellation.
- Save to `Downloads/Local Media` using Android MediaStore; open recent files in a player.
- Android 10+; ARM64 device and x86_64 emulator ABIs. Target Android 16.

Internet access to YouTube is necessary. Processing is local, not offline downloading. Restricted, private, DRM-protected or unavailable videos are not bypassed. YouTube changes and server-side restrictions can cause downloads to fail; exact engine errors are visible in the app.

Current compatibility trial: 0.1.1 / versionCode 2 / `com.inchat.localmedia`. Engineering validation and actual phone validation are separate.

Read ARCHITECTURE.md, BUILD_AND_RELEASE.md and DEVICE_TEST_PLAN.md.
