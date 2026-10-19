"""Fresh CI emulator only: MAIN3000003/Room22 -> MAIN3000004/Room23, no production key."""
from pathlib import Path
from runpy import run_path

h = run_path(str(Path(__file__).with_name('run-build-isolation-checks.py')))
adb = h['adb']
APP = 'ru.fo6osik.workjournal'


def main():
    if adb('shell', 'getprop', 'ro.kernel.qemu').strip() != '1':
        raise RuntimeError('Disposable emulator required')
    packages = set(adb('shell', 'pm', 'list', 'packages').splitlines())
    if any('package:' + p in packages for p in [APP, APP+'.test', APP+'.debugtest']):
        raise RuntimeError('Refusing any existing MAIN/TEST installation')
    baseline = '.main-room22/app/build/outputs/apk/debug/app-debug.apk'
    current = 'app/build/outputs/apk/debug/app-debug.apk'
    tests = 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
    for path in [baseline, current, tests]:
        assert Path(path).is_file(), path
    adb('install', baseline)
    adb('install', tests)
    h['instrument'](APP, 'seedMain107Room22', 'MainRoom23MigrationTest')
    adb('install', '-r', current)
    h['instrument'](APP, 'verifyMain108Room23AndSeparateRestore', 'MainRoom23MigrationTest')
    adb('shell', 'am', 'force-stop', APP)
    h['instrument'](APP, 'verifyMain108Room23AndSeparateRestore', 'MainRoom23MigrationTest')
    print('MAIN ROOM22->23 SUCCESS: 1.0.7/3000003 -> 1.0.9/3000004; all old columns/tables, active states, fuel, events, files/preferences/timers and certificate retained; separate-database restore and restart verified. Disposable CI debug certificate only.')


if __name__ == '__main__':
    main()
