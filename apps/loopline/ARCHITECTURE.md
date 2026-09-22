# Architecture

One Android Activity hosts bundled HTML/CSS/JavaScript in a WebView. It requires no external runtime services. The puzzle generator uses a deterministic seeded pseudo random number generator. Daily seeds include the local calendar date, mode, stage and generator version; free play uses the saved stage index. Path generation performs randomized depth first search for a self avoiding route and marks its ordered anchors; all obstacles lie outside that route. Sum generation shuffles digits 1–9 and derives six target sums from a valid permutation; any arrangement meeting all sums succeeds. Memory sequences are generated from the seed.

The only persisted state is `loopline-v1` in local storage: daily progress, free play index and audio preference. Resetting app data removes progress. No JavaScript bridge is exposed. No network permission is requested.
