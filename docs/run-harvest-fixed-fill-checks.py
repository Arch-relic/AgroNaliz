"""Fresh emulator only: pinned MAIN1.0.10 -> candidate1.0.11, both disposable debug packages."""
import json
import os
from pathlib import Path
from runpy import run_path
h=run_path(str(Path(__file__).with_name('run-build-isolation-checks.py')))
identity=run_path(str(Path(__file__).with_name('run-main-room24-same-checks.py')))
adb=h['adb']


def main():
    if adb('shell','getprop','ro.kernel.qemu').strip()!='1': raise RuntimeError('Disposable emulator required')
    installed=adb('shell','pm','list','packages').splitlines()
    reports=[]
    for variant,app in [('debug','ru.fo6osik.workjournal'),('debugTest','ru.fo6osik.workjournal.debugtest')]:
        if any('package:'+p in installed for p in [app,app+'.test']): raise RuntimeError('Refuse existing data')
        old=Path(f'.main110/app/build/outputs/apk/{variant}/app-{variant}.apk')
        new=Path(f'app/build/outputs/apk/{variant}/app-{variant}.apk')
        pin=identity['inspect'](old,app,3000008 if variant=='debug' else 1000301,'1.0.10')
        if identity['inspect'](new,app,3000009 if variant=='debug' else 1000302,'1.0.11')!=pin:
            raise RuntimeError('Disposable fixture certificate mismatch')
        adb('install',str(old))
        adb('install',f'app/build/outputs/apk/androidTest/{variant}/app-{variant}-androidTest.apk')
        h['instrument'](app,'seedBaseline110','HarvestFixedFillUpgradeTest')
        adb('install','-r',str(new))
        h['instrument'](app,'verifyUpgradeAndLegacyBackup','HarvestFixedFillUpgradeTest')
        adb('shell','am','force-stop',app)
        h['instrument'](app,'verifyUpgradeAndLegacyBackup','HarvestFixedFillUpgradeTest')
        h['instrument'](app,'editOldBunkerAndRestoreWithoutChangingOtherFields','HarvestFixedFillUpgradeTest')
        h['instrument'](app,'quantitiesStayTogetherOnSmallScreens','HarvestFixedFillUpgradeTest')
        h['instrument'](app,'verifyUpgradeAndLegacyBackup','HarvestFixedFillUpgradeTest')
        reports.append(dict(applicationId=app,schemaVersion=24,schemaUnchanged=True,rowsUnchanged=True,
                            legacyBackupRestored=True,restart=True,oldVersion='1.0.10',newVersion='1.0.11'))
    Path('harvest-fixed-fill-verification.json').write_text(json.dumps(dict(sourceSha=os.environ['GITHUB_SHA'],
        baselineSha='2a37bfe05c9b60f04035f7b64fae7f726117d647',fixtures=reports,productionReleaseAuthorized=False),indent=2)+'\n')


if __name__=='__main__': main()
