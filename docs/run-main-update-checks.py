"""Owner data never accessed: in-place main updates on an empty disposable emulator."""
import os
from pathlib import Path
import subprocess
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
    _,pin=helpers['fixture_key'](None,None)
    if pin == '3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C':
        raise RuntimeError('Production certificate forbidden in disposable update tests')
    sdk=helpers['sdk']()
    def verify(path, expected_code, expected_version=None):
        badging=subprocess.check_output([str(sdk/'aapt2'),'dump','badging',str(path)],text=True)
        if helpers['PROGRAM']['apk_identity'](badging)!=(APP,expected_code):
            raise RuntimeError('Unexpected synthetic MAIN package/versionCode')
        if expected_version and f"versionName='{expected_version}'" not in badging:
            raise RuntimeError('Unexpected synthetic MAIN versionName')
        verified=subprocess.check_output([str(sdk/'apksigner'),'verify','--print-certs',str(path)],text=True)
        if helpers['PROGRAM']['certificate_digest'](verified)!=pin:
            raise RuntimeError('Synthetic update must retain the disposable debug certificate')
    # Gradle has already signed these fixtures with the same disposable debug certificate.
    # Do not route an unapproved product through the production signer/admission protocol.
    first=Path(os.environ['RUNNER_TEMP'])/'main-update-input/first.apk'
    second=Path(os.environ['RUNNER_TEMP'])/'main-update-input/second.apk'
    baseline=Path('.main-v20/app/build/outputs/apk/debug/app-debug.apk')
    verify(baseline,53);verify(first,3000011,'1.0.11');verify(second,3000012,'1.0.11')
    adb('install',str(baseline))
    adb('install','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')
    instrument('seedV20')
    adb('install','-r',str(first));instrument('verifyV21Update')
    adb('install','-r',str(second));adb('shell','am','force-stop',APP)
    instrument('verifySecondUpdateAndRestart')
    print('MAIN UPDATE SUCCESS: version53 Room20 -> MAIN1.0.11 Room24 -> higher-code MAIN1.0.11 Room24; all stored legacy values, preferences, timers, files and certificate retained; new area/density retained after restart; no uninstall/clear/downgrade flags')

if __name__=='__main__':main()
