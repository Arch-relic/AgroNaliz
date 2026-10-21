"""Fresh emulator, disposable debug certificates, synthetic databases only. Never authorizes release."""
import hashlib
import json
import os
from pathlib import Path
import re
from runpy import run_path
import subprocess

h=run_path(str(Path(__file__).with_name('run-build-isolation-checks.py')))
adb=h['adb']
sdk=Path(os.environ['ANDROID_HOME'])/'build-tools/35.0.0'
PIN='3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C'


def inspect(path, app, code, version):
    text=subprocess.check_output([str(sdk/'aapt2'),'dump','badging',str(path)],text=True)
    match=re.search(r"^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'",text,re.M)
    if not match or match.groups()!=(app,str(code),version):
        raise RuntimeError('Fixture identity mismatch')
    text=subprocess.check_output([str(sdk/'apksigner'),'verify','--print-certs',str(path)],text=True)
    pins=re.findall(r'^Signer #\d+ certificate SHA-256 digest: ([a-fA-F0-9]{64})$',text,re.M)
    if len(pins)!=1 or pins[0].upper()==PIN:
        raise RuntimeError('Only disposable CI certificate permitted')
    return pins[0].upper()


def main():
    if adb('shell','getprop','ro.kernel.qemu').strip()!='1': raise RuntimeError('Fresh CI emulator required')
    packages=adb('shell','pm','list','packages').splitlines()
    for app in ['ru.fo6osik.workjournal','ru.fo6osik.workjournal.debugtest']:
        if 'package:'+app in packages or 'package:'+app+'.test' in packages: raise RuntimeError('Refuse any existing data')
    baseline=json.loads(Path("docs/main109-baseline.json").read_text())
    if baseline["sourceSha"] != "6da84bf09cf0cd3e9d1b5a40887a134bf25d7fe4" or baseline["versionCode"] != 3000007 or baseline["roomVersion"] != 24 or baseline["versionName"] != "1.0.9" or baseline["certificateSha256"] != PIN:
        raise RuntimeError("Published baseline evidence changed; audit needs review")
    reports=[]
    for variant,app,oldcode,newcode in [('debug','ru.fo6osik.workjournal',3000007,3000008),('debugTest','ru.fo6osik.workjournal.debugtest',1000301,1000302)]:
        old=Path(f'.main109/app/build/outputs/apk/{variant}/app-{variant}.apk')
        new=Path(f'app/build/outputs/apk/{variant}/app-{variant}.apk')
        tests=Path(f'app/build/outputs/apk/androidTest/{variant}/app-{variant}-androidTest.apk')
        pin=inspect(old,app,oldcode,'1.0.9')
        if inspect(new,app,newcode,'1.0.11')!=pin: raise RuntimeError('Certificate mismatch')
        adb('install',str(old));adb('install',str(tests))
        h['instrument'](app,'seed24','Room24UpdateTest')
        adb('install','-r',str(new))
        h['instrument'](app,'verify24SameAndSeparateRestore','Room24UpdateTest')
        adb('shell','am','force-stop',app)
        h['instrument'](app,'verify24SameAndSeparateRestore','Room24UpdateTest')
        h['instrument'](app,'verify109Screens','Main109UpdateUiTest')
        h['instrument'](app,'verify24SameAndSeparateRestore','Room24UpdateTest')
        reports.append(dict(applicationId=app,oldRoom=24,newRoom=24,oldVersionName='1.0.9',newVersionName='1.0.11',oldVersionCode=oldcode,fixtureVersionCode=newcode,
            ciCertificateSha256=pin,apkSha256=hashlib.sha256(new.read_bytes()).hexdigest(),dataRetained=True,separateRestore=True,restart=True))
    Path('main-room24-same-verification.json').write_text(json.dumps(dict(sourceSha=os.environ['GITHUB_SHA'],fixtures=reports,publishedBaseline=baseline,schemaUnchanged=True,productionReleaseAuthorized=False),indent=2)+'\n')
    print('ROOM24 SAME-SCHEMA UPDATE SUCCESS: both synthetic packages retain all old data, files, preferences and certificate; JSON restore and restart passed; release not authorized')


if __name__=='__main__': main()
