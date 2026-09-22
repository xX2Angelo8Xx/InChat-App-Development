# Loopline

An offline Android puzzle game with three mechanics: trace numbered checkpoints without revisiting a cell, exchange digits to satisfy all row/column sums, and reproduce a brief visual sequence. Three deterministic daily puzzles use the device's local date; free play generates an unbounded sequence. Progress is held only in WebView local storage. No account, analytics, network permission or advertising.

The main Android entry point is `android/app/src/main/java/com/inchat/loopline/MainActivity.java`; game code and presentation are in `android/app/src/main/assets/`.
