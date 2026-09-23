# Loopline

An offline Android puzzle game with three mechanics: trace numbered checkpoints while covering every free cell, exchange digits to satisfy all row/column sums, and reproduce a visual sequence forwards or backwards. Three deterministic daily puzzles use the device's local date; free play offers independent levels per mode. Stars, time, swaps and personal bests are held only in WebView local storage. No account, analytics, network permission or advertising.

The main Android entry point is `android/app/src/main/java/com/inchat/loopline/MainActivity.java`; game code and presentation are in `android/app/src/main/assets/`.
