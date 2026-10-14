"""Owner data never accessed: in-place main updates on an empty disposable emulator."""
import os
from pathlib import Path
import subprocess
import tempfile
from runpy import run_path

ROOT=Path(__file__).resolve().parents[1]
isolation=run_path(str(ROOT/'docs/run-build-isolation-checks.py'))
adb=isolation['adb']
helpers=run_path(str(ROOT/'docs/test_main_apk_release.py'))
APP='ru.fo6osik.workjournal'

def instrument(method):
    out=adb('shell','am','instrument','-w','-e','class','ru.fo6osik.workjournal.MainUpdateTest#'+method,
            APP+'.test/androidx.test.runner.AndroidJUnitRunner')
    if 'OK (1 test)' not in out or 'FAILURES!!!' in out:raise RuntimeError('Main update data check failed: '+method)

def main():
    if adb('shell','getprop','ro.kernel.qemu').strip()!='1':raise RuntimeError('Disposable emulator required')
    packages=adb('shell','pm','list','packages').splitlines()
    if any('package:'+p in packages for p in [APP,APP+'.test',APP+'.debugtest']):
        raise RuntimeError('Refusing to replace an existing installation')
    key,pin=helpers['fixture_key'](None,None)
    with tempfile.TemporaryDirectory(prefix='main-update-fixture-',dir=os.environ['RUNNER_TEMP']) as folder:
        root=Path(folder);signed=[]
        for name in ['first','second']:
            dest=root/name;dest.mkdir()
            signed.append(helpers['sign_fixture'](Path(os.environ['RUNNER_TEMP'])/'main-update-input'/ (name+'.apk'),dest,key,pin))
        # All APKs share the already-generated disposable runner debug key. No production secrets.
        adb('install','.main-v20/app/build/outputs/apk/debug/app-debug.apk')
        adb('install','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')
        instrument('seedV20')
        adb('install','-r',str(signed[0]));instrument('verifyV21Update')
        adb('install','-r',str(signed[1]));adb('shell','am','force-stop',APP)
        instrument('verifySecondUpdateAndRestart')
    print('MAIN UPDATE SUCCESS: version53 Room20 -> higher version Room21 -> higher version Room21; all stored legacy values, preferences, timers, files and certificate retained; new area/density retained after restart; no uninstall/clear/downgrade flags')

if __name__=='__main__':main()
