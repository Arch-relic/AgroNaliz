"""Room22 ->23 and harvest 1.0.8, only in a fresh disposable TEST emulator."""
from pathlib import Path
from runpy import run_path

h = run_path(str(Path(__file__).with_name('run-harvest-checks.py')))
adb = h['adb']
instrument = h['instrument']
APP = 'ru.fo6osik.workjournal.debugtest'


def main():
    if adb('shell', 'getprop', 'ro.kernel.qemu').strip() != '1':
        raise RuntimeError('Disposable emulator only')
    if 'package:' + APP in adb('shell', 'pm', 'list', 'packages').splitlines():
        raise RuntimeError('Refusing existing TEST data')
    current = 'app/build/outputs/apk/debugTest/app-debugTest.apk'
    tests = 'app/build/outputs/apk/androidTest/debugTest/app-debugTest-androidTest.apk'
    adb('shell', 'settings', 'put', 'system', 'screen_off_timeout', '2147483647')
    adb('shell', 'svc', 'power', 'stayon', 'true')
    adb('install', current)
    adb('install', '-r', tests)
    instrument('HarvestQuickAccessUiTest', count=2)
    instrument('Harvest108Test', count=5)
    instrument('HarvestFullBackup108Test', count=4)
    adb('uninstall', APP)
    adb('install', '.migration-v22/app/build/outputs/apk/debugTest/app-debugTest.apk')
    # Test APK is kept by Android after the target APK is removed; reinstall explicitly.
    adb('install', '-r', tests)
    instrument('HarvestRoom23MigrationTest', 'seed22')
    adb('install', '-r', current)
    instrument('HarvestRoom23MigrationTest', 'verify23AllValuesAndRestart')
    adb('shell', 'am', 'force-stop', APP)
    instrument('HarvestRoom23MigrationTest', 'verify23AllValuesAndRestart')
    adb('uninstall', APP)
    print('HARVEST 1.0.8: completion rollback, independent areas, quick access, '
          'separate-database JSON roundtrip/merge, Room22->23 and restart passed')


if __name__ == '__main__':
    main()
