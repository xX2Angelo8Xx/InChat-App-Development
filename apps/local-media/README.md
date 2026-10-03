# Local Media

Standalone Android app: paste/share a YouTube link → Suchen → title/thumbnail preview → MP3 or MP4 and available source quality → download. Processing stays on the device, with bundled yt-dlp/Python/QuickJS/EJS and FFmpeg. No backend, account, analytics, Termux or separate runtime installation.

Search retrieves individual-video metadata and available formats. MP3 offers actual audio-source bitrate/codec choices, converted locally to MP3 at 192 kbit/s without implying improved source quality. MP4 offers available H.264/MP4 resolutions and frame rates with audio. Unsupported codecs and protected tracks are not advertised. Thumbnail retrieval is optional, bounded, and directly from YouTube image hosts; its failure does not prevent download.

Downloads use explicit track IDs, isolated app-private durable job folders, and separate FFmpeg conversion/muxing. Files are validated and published through MediaStore to Downloads/Local Media. Progress, cancellation and private recent-file history are included.

Android 10+, ARM64 and x86_64; target Android 16. Version 0.2.0 / versionCode 3 / com.inchat.localmedia. Same public playtest certificate, v1 + v2.

Internet access to YouTube is necessary. Restricted, private, DRM-protected or unavailable videos are not bypassed. YouTube changes and access restrictions can cause metadata/download failures. Re-search if offered formats become unavailable.

Phone feedback: installation/start worked after a phone reboot. The previous v1 signing trial did not independently resolve the parsing error. Initial live audio download then failed during yt-dlp audio post-processing with a missing WebM original. 0.2.0 replaces that post-processing path; phone retest remains required.

Read ARCHITECTURE.md, BUILD_AND_RELEASE.md and DEVICE_TEST_PLAN.md.
