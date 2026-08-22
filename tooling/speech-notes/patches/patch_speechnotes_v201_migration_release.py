from pathlib import Path

p = Path('SpeechNotes/app/build.gradle')
s = p.read_text()

if 'versionCode 20' not in s or 'versionName "2.0.0"' not in s:
    raise SystemExit('Expected v2.0.0 version block not found after patch chain')

s = s.replace('versionCode 20', 'versionCode 21', 1)
s = s.replace('versionName "2.0.0"', 'versionName "2.0.1"', 1)
p.write_text(s)

print('Applied v2.0.1 monorepo migration release version bump (versionCode 21)')
