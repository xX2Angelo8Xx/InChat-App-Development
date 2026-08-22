from pathlib import Path
import re

p = Path('SpeechNotes/app/build.gradle')
s = p.read_text()

if not re.search(r"versionName\s+['\"]2\.0\.0['\"]", s):
    raise SystemExit('Expected v2.0.0 versionName not found after patch chain')

m = re.search(r'versionCode\s+(\d+)', s)
if not m:
    raise SystemExit('Expected versionCode not found after patch chain')

current_code = int(m.group(1))
if current_code != 20:
    raise SystemExit(f'Expected v2.0.0 versionCode 20 after patch chain, got {current_code}')

s = re.sub(r"versionName\s+['\"]2\.0\.0['\"]", "versionName '2.0.1'", s, count=1)
s = s[:m.start()] + 'versionCode 21' + s[m.end():]
p.write_text(s)

print('Applied v2.0.1 monorepo migration release version bump (versionCode 21)')
