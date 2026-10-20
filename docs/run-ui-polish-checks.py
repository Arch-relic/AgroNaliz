"""Fresh disposable emulator only; no production key, APK publication or existing data."""
from pathlib import Path
from runpy import run_path

h = run_path(str(Path(__file__).with_name('run-build-isolation-checks.py')))
adb = h['adb']


def main():
    if adb('shell', 'getprop', 'ro.kernel.qemu').strip() != '1':
        raise RuntimeError('Disposable emulator required')
    packages = adb('shell', 'pm', 'list', 'packages').splitlines()
    for variant, app in [('debug', 'ru.fo6osik.workjournal'), ('debugTest', 'ru.fo6osik.workjournal.debugtest')]:
        if 'package:' + app in packages or 'package:' + app + '.test' in packages:
            raise RuntimeError('Refuse existing data')
        adb('install', f'app/build/outputs/apk/{variant}/app-{variant}.apk')
        adb('install', f'app/build/outputs/apk/androidTest/{variant}/app-{variant}-androidTest.apk')
        for method in ['installedDiagnosticsBrandingCopyAndNoDatabaseChanges', 'inaccessiblePackageMetadataReturnsUnknown',
                       'numericRangesRemainWholeAtDifferentWidthsAndFontScales']:
            h['instrument'](app, method, 'UiPolishTest')
    print('UI POLISH SUCCESS: both packages, installed metadata/copy, no database changes, numeric ranges and large fonts')


if __name__ == '__main__':
    main()
