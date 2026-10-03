#!/usr/bin/env bash
set -euo pipefail
ROOT="$(git rev-parse --show-toplevel)"
DEST="$ROOT/apps/local-media/android/app/src/main/res/raw/ytdlp"
mkdir -p "$(dirname "$DEST")"
curl --fail --location --retry 3 --max-time 180 'https://github.com/yt-dlp/yt-dlp/releases/download/2026.08.19/yt-dlp' -o "$DEST"
echo "1fa6733c37ea6fb51c99ad8fe785e7b7e5f3246c9b980230329d4fb72ed8d4d6  $DEST" | sha256sum --check
python3 - "$DEST" <<'PY'
import sys, zipfile
with zipfile.ZipFile(sys.argv[1]) as z:
    assert "2026.08.19" in z.read("yt_dlp/version.py").decode()
    assert "yt_dlp_ejs/yt/solver/core.min.js" in z.namelist()
    assert "yt_dlp_ejs/yt/solver/lib.min.js" in z.namelist()
PY
