import sys, zipfile, hashlib, io
apk = sys.argv[1]
with zipfile.ZipFile(apk) as z:
    names = set(z.namelist())
    for abi in ('arm64-v8a', 'x86_64'):
        for lib in ('libpython.so', 'libpython.zip.so', 'libqjs.so', 'libffmpeg.so', 'libffmpeg.zip.so'):
            assert f'lib/{abi}/{lib}' in names, (abi, lib)
        with zipfile.ZipFile(io.BytesIO(z.read(f'lib/{abi}/libpython.zip.so'))) as python_runtime:
            assert 'usr/lib/libc++_shared.so' in python_runtime.namelist()
    assert not any(n.startswith('lib/armeabi-v7a/') or n.startswith('lib/x86/') for n in names)
    engine = z.read('res/raw/ytdlp')
    assert hashlib.sha256(engine).hexdigest() == '1fa6733c37ea6fb51c99ad8fe785e7b7e5f3246c9b980230329d4fb72ed8d4d6'
    with zipfile.ZipFile(io.BytesIO(engine)) as nested:
        assert 'yt_dlp_ejs/yt/solver/core.min.js' in nested.namelist()
print('APK native runtimes, ABIs, pinned yt-dlp and bundled EJS verified')
