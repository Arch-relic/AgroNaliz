"""Room21 ->22 and fuel/moisture checks, only after guarded isolation/harvest fixtures."""
from pathlib import Path
from runpy import run_path
h=run_path(str(Path(__file__).with_name('run-harvest-checks.py')))
adb=h['adb'];instrument=h['instrument'];APP='ru.fo6osik.workjournal.debugtest'
def main():
    if adb('shell','getprop','ro.kernel.qemu').strip()!='1':raise RuntimeError('Disposable emulator only')
    if 'package:'+APP in adb('shell','pm','list','packages').splitlines():raise RuntimeError('Refusing existing TEST data')
    adb('install','.migration-v21/app/build/outputs/apk/debugTest/app-debugTest.apk')
    adb('install','-r','app/build/outputs/apk/androidTest/debugTest/app-debugTest-androidTest.apk')
    instrument('HarvestFuelTest','seed21')
    adb('install','-r','app/build/outputs/apk/debugTest/app-debugTest.apk')
    for method in ['verify22','completedEditsAndReopenPreserve','betweenDaysIsIndependent','newBackupAndMergeRoundTrip','legacyBackupAndImport','invalidValuesRollback']:
        instrument('HarvestFuelTest',method)
    instrument('HarvestFuelUiTest',count=5)
    adb('shell','am','force-stop',APP)
    instrument('HarvestFuelTest','restartPreserved')
    adb('uninstall',APP)
    h['isolation']['instrument']('ru.fo6osik.workjournal','verifyMainAfterTest')
    print('FUEL SUCCESS: Room21->22 preserves existing harvest; moisture edit/clear; daily/between-day refills, completed edits, backup22 and legacy21 merge/restore, restart and main isolation verified')
if __name__=='__main__':main()
