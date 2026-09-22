# Build and release

From `android/`: `gradle :app:assembleDebug :app:lintDebug`. A debug APK can be installed on a device with `adb install -r app/build/outputs/apk/debug/app-debug.apk`. The `loopline.yml` workflow runs generator checks, Android build, lint and artifact upload from a clean checkout. Its debug artifact is for playtesting; it is not a production signed release. Package identity: `com.inchat.loopline`, version 1.0.0 (code 1), min API 26.

Device acceptance: launch without a network; complete all three daily puzzles; drag through several path cells in one gesture and backtrack; restart a memory round during playback; background/foreground; reopen to check progress; verify the next local calendar day has three new puzzles; play 10+ free stages. Device testing remains necessary after CI.
