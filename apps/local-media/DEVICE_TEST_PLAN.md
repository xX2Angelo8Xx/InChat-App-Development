# Phone acceptance (pending)

Target: Galaxy A56 5G / Android 16 / ARM64.

1. Retest 0.1.1 from Samsung My Files: installer must display Local Media rather than Unknown and reach install confirmation. Record whether the immediate parsing failure from 0.1.0 persists. This is a signing compatibility trial, not an established fix. Install APK, cold start with airplane mode: UI starts without runtime download or sign-in.
2. Restore Internet; paste a publicly accessible permitted test video. Download MP3; confirm file appears under Downloads/Local Media and plays as audio.
3. Download the same video as MP4 at 720p; confirm both picture and audio. Test 360p and 1080p when available.
4. Share a video from YouTube; link populates without initiating download automatically.
5. Repeat download, including a second file of the same video; Android MediaStore keeps distinct completed entries.
6. Cancel during preparation, download and conversion; no completed entry for unfinished output; restart succeeds.
7. Rotate during download; settings and live progress survive Activity recreation. Background and lock screen; foreground notification reflects the running job.
8. Deny notification permission; download works. Grant it later; verify notification and cancel action.
9. Disconnect Wi-Fi during download, invalid URL, unavailable video, unsupported/private video, low storage: visible error, no false success, retry works.
10. Open history, delete file externally, then try opening its history entry: handled error.
11. Force-stop during staging/copy; restart and retry: no duplicate worker, clean pending output.

CI checks real Python/yt-dlp/QuickJS startup and synthetic local FFmpeg MP3/MP4 creation on an x86_64 Android 16 emulator plus MediaStore publication. It does not prove live YouTube availability or ARM64 runtime behavior.

CI also reads the built release archive through PackageManager using both signature APIs, verifies version/certificate and loads name/icon. This still does not emulate Samsung Package Installer.

## 0.2.0 acceptance

1. Update the installed app without uninstalling (same signing identity); cold start.
2. Paste/share a link: format selection and download must wait for Suchen. Search shows matching title and thumbnail; image failure is nonfatal.
3. Inspect MP3 audio-source choices and MP4 available H.264 resolutions/frame rates. Formats not provided by the source must not be listed; MP3 output is 192 kbit/s.
4. Change the link: previous preview must disappear and download be unavailable until the new search succeeds. Rotate during search and after preview; selection/UI must remain coherent. Force-stop/relaunch requires re-search.
5. Repeat the originally failing live URL as MP3 (OETnuwwsv9U), then MP4 including separate video/audio sources. Verify playback and saved filename.
6. Cancel a search, each track download and FFmpeg conversion; retry. Background/lock during download; notify permission denied/granted. Disconnect network during metadata/track download.
7. Force-stop during transfer/conversion/publication, then restart/retry: no duplicate worker or pending output, durable orphan work recovered by next job.

CI covers metadata fixtures, real bundled engine file-URL download and FFmpeg MP3/MP4 pipeline plus MediaStore, populated preview UI and invalidation, and final-release parsing/signing/install checks. It does not establish why the original WebM was missing or prove live YouTube/ARM64 acceptance.
