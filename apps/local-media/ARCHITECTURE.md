# Architecture

Native Kotlin Activity and a single foreground dataSync Service. No WebView or remote backend.

Data flow: validated YouTube URL → bundled yt-dlp/Python and QuickJS/EJS → local download → bundled FFmpeg conversion or muxing → app-private staging file → MediaStore pending row → atomic visibility in Downloads → local history.

All expensive extraction, initialization, media conversion and file copying run on one bounded service executor. A foreground notification and bounded two-hour wake lock maintain execution while the screen is off. Android's dataSync foreground timeout stops the service. Exactly one job is allowed. Main-thread state delivery uses a process-local observable state; the Activity subscribes only while visible. Service owns process cancellation, private staging and cleanup.

MediaStore requires no broad storage permission. MP3/MP4 file signatures are checked before publication, rejecting HTML error pages returned with HTTP 200 instead of falsely recording success. A failed copy deletes the pending row; the next download recovers this app's unfinished pending rows and old staging. Completed files persist after app removal; history is capped to 20 URI entries in private preferences. Backup is disabled. Process death stops a job without automatic retry, resumption or falsely recording success; user restarts it. No persistent URL logs or telemetry.

Input accepts only HTTPS exact YouTube hosts and known individual-video URL shapes. Playlists are stripped to the individual video. Arguments go to the library as a list, never a shell command. No credential entry, cookies, proxies, third-party converters or remote EJS fetching are implemented. Failure is surfaced with the engine diagnostic.

MP4 prefers H.264 MP4 + M4A audio, with a combined MP4 fallback, and merges/remuxes locally. It never silently returns WebM under an MP4 extension. An unavailable supported stream raises an error. MP3 is encoded at 192 kbit/s without claiming to improve the source.

Pinned runtime: youtubedl-android and FFmpeg 0.18.1; app-level raw resource overrides the library's older yt-dlp zipapp with official yt-dlp 2026.08.19, SHA-256 verified. Its EJS solvers are bundled. No network work happens on app launch. Downloading the pinned resource is a reproducible build input preparation step, not a code patch chain.

Permissions: INTERNET, normal foreground-service/dataSync and wake-lock permissions; optional notification permission requested when the first job is started. Notification denial does not block download. No all-files access, microphone, location, contacts or account permissions.
