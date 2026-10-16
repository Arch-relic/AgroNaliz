"""Actual same-key TEST updates, different-key rejection and read-only backup export on CI emulator.
Run only after run-build-isolation-checks.py created synthetic main and removed its TEST fixture.
"""
import importlib.util
import json
import os
import re
from pathlib import Path
import shutil
import subprocess
import tempfile
from runpy import run_path

ROOT=Path(__file__).resolve().parents[1]
helpers=run_path(str(Path(__file__).with_name('test_test_apk_signing.py')))
isolation=run_path(str(Path(__file__).with_name('run-build-isolation-checks.py')))
adb=isolation['adb']
APP='ru.fo6osik.workjournal.debugtest'


def instrument(method):
    output=adb('shell','am','instrument','-w','-e','class','ru.fo6osik.workjournal.TestSigningUpdateTest#'+method,
               APP+'.test/androidx.test.runner.AndroidJUnitRunner')
    if 'OK (1 test)' not in output or 'FAILURES!!!' in output:
        raise RuntimeError('TEST signing data regression failed: '+method)


def main():
    if adb('shell','getprop','ro.kernel.qemu').strip()!='1':
        raise RuntimeError('Disposable emulator required; never use a user phone')
    if 'package:'+APP in adb('shell','pm','list','packages').splitlines():
        raise RuntimeError('Refusing to replace an existing TEST installation')
    isolation['instrument']('ru.fo6osik.workjournal','verifyMainAfterTest')
    # This companion was installed by the preceding guarded isolation setup, not by a user.
    adb('uninstall',APP+'.test')
    versions=Path(os.environ['RUNNER_TEMP'])/'test-update-input'
    with tempfile.TemporaryDirectory(prefix='test-signing-fixture-',dir=os.environ['RUNNER_TEMP']) as folder:
        root=Path(folder);key_a,pin_a=helpers['fixture_key'](root,'a');key_b,pin_b=helpers['fixture_key'](root,'b')
        signed=[]
        for name,key,pin in [('first',key_a,pin_a),('second',key_a,pin_a),('conflict',key_b,pin_b)]:
            target=root/name;target.mkdir()
            source=versions/('first.apk' if name=='first' else 'second.apk')
            signed.append(helpers['sign_fixture'](source,target,key,pin))
        test_source=ROOT/'app/build/outputs/apk/androidTest/debugTest/app-debugTest-androidTest.apk'
        test_apk=root/'fixture-instrumentation.apk'
        env=dict(os.environ,FIXTURE_PASSWORD='synthetic-password')
        subprocess.run([str(helpers['sdk']()/'apksigner'),'sign','--ks',str(key_a),'--ks-key-alias','fixture-alias',
                        '--ks-pass','env:FIXTURE_PASSWORD','--key-pass','env:FIXTURE_PASSWORD',
                        '--v1-signer-name','TEST','--out',str(test_apk),str(test_source)],
                       env=env,check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        adb('install',str(signed[0]));adb('install',str(test_apk));instrument('seedDataAndExport')
        # Copy an existing exported JSON through the same read-only owner helper.
        names=adb('shell','run-as',APP,'ls','files/test-backups').splitlines()
        if len(names)!=1:raise RuntimeError('Unexpected synthetic backup count')
        serial=subprocess.check_output(['adb','get-serialno'],text=True).strip()
        export=run_path(str(Path(__file__).with_name('export-installed-test-backup.py')))
        raw=export['read_backup']('adb',serial,names[0])
        target_room=int(re.search(r'version\s*=\s*(\d+)',(ROOT/'app/src/main/java/ru/fo6osik/workjournal/AppDatabase.kt').read_text()).group(1))
        version,counts=export['validate_backup'](raw,target_room)
        copy=root/'portable-copy.json';export['save_copy'](raw,copy)
        if copy.read_bytes()!=raw:raise RuntimeError('Backup copy changed')
        adb('install','-r',str(signed[1]));instrument('verifyAfterSameKeyUpdate')
        isolation['instrument'](APP,'showsInstalledVersion','AppVersionUiTest')
        rejected=subprocess.run(['adb','install','-r',str(signed[2])],capture_output=True,text=True)
        if rejected.returncode==0 or 'INSTALL_FAILED_UPDATE_INCOMPATIBLE' not in rejected.stdout+rejected.stderr:
            raise RuntimeError('Android must reject a different TEST certificate')
        instrument('verifyAfterRejectedDifferentKeyUpdate');instrument('verifySyntheticBackupRestore')
    isolation['instrument']('ru.fo6osik.workjournal','verifyMainAfterTest')
    print('TEST SIGNING SUCCESS: same key update retains Room/preferences/files; different key rejected with data intact; private JSON read-only export and synthetic restore verified; main unchanged')


if __name__=='__main__':main()
