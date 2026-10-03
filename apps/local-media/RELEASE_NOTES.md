Local Media 0.2.0 — search, preview and source quality

Paste/share a YouTube link, press Suchen, inspect title and thumbnail, then choose MP3/MP4 and actual available audio-source quality or H.264 MP4 resolution. A changed link invalidates the old preview. MP3 is encoded locally at 192 kbit/s; selecting a source does not upscale its quality.

Replaces the failing yt-dlp audio post-processing path with separate track downloads and explicit bundled FFmpeg conversion/muxing. Durable per-job working directories and distinct source/output files keep inputs intact until publication. Completed MP3/MP4 headers are validated before saving to Downloads/Local Media. Cancellation covers extraction/download and FFmpeg; error details can be copied.

Same package/playtest certificate, Android 10+, ARM64/x86_64, v1+v2 signing, versionCode 3. Compilation/lint/unit/Android offline runtime, populated preview, format parsing, end-to-end WebM→MP3→MediaStore and MP4 muxing, final release parsing/install/signing/package checks run before publication. Phone/live YouTube retest remains pending. The former installation issue was reported resolved after a phone reboot; its exact cause was not established.
