"""Focused offline screen checks; never operate on an existing install or physical phone."""
from pathlib import Path
from runpy import run_path

h = run_path(str(Path(__file__).with_name('run-harvest-checks.py')))
adb, instrument = h['adb'], h['instrument']
APP = 'ru.fo6osik.workjournal.debugtest'


def main():
    if adb('shell', 'getprop', 'ro.kernel.qemu').strip() != '1':
        raise RuntimeError('Disposable emulator only')
    if 'package:' + APP in adb('shell', 'pm', 'list', 'packages').splitlines():
        raise RuntimeError('Refusing existing TEST data')
    adb('shell', 'settings', 'put', 'system', 'screen_off_timeout', '2147483647')
    adb('shell', 'svc', 'power', 'stayon', 'true')
    adb('install', 'app/build/outputs/apk/debugTest/app-debugTest.apk')
    adb('install', 'app/build/outputs/apk/androidTest/debugTest/app-debugTest-androidTest.apk')
    instrument('ChangelogUiTest', 'navigationIndependentExpansionAndRecreation')
    instrument('ChangelogUiTest', 'offlineAndNoDatabaseChanges')
    font = adb('shell', 'settings', 'get', 'system', 'font_scale').strip()
    try:
        adb('shell', 'wm', 'size', '720x960')
        adb('shell', 'settings', 'put', 'system', 'font_scale', '1.6')
        adb('shell', 'am', 'force-stop', APP)
        instrument('ChangelogUiTest', 'compactLargeFontScreenshot')
        Path('changelog-screenshots').mkdir(exist_ok=True)
        adb('pull', '/sdcard/Android/data/' + APP + '/files/changelog-screenshots/history.png',
            'changelog-screenshots/history.png')
    finally:
        adb('shell', 'wm', 'size', 'reset')
        if font == 'null':
            adb('shell', 'settings', 'delete', 'system', 'font_scale')
        else:
            adb('shell', 'settings', 'put', 'system', 'font_scale', font)
        adb('shell', 'am', 'force-stop', APP)
    print('CHANGELOG SUCCESS: navigation, independent state, recreation, offline, unchanged database and large-font screenshot')


if __name__ == '__main__':
    main()
