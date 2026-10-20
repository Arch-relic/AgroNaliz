"""Read-only check of a downloaded original main APK against the installed main package.
Never installs/uninstalls, clears data, opens the live database or changes the phone.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile

APP='ru.fo6osik.workjournal'
CERT='3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C'


def tool(command):
    result=subprocess.run(command,capture_output=True,text=True,timeout=120)
    if result.returncode:raise ValueError('Read-only verification tool failed; private output suppressed')
    return result.stdout


def metadata(apk,aapt,signer):
    text=tool([signer,'verify','--print-certs',str(apk)])
    pins=re.findall(r'^Signer #\d+ certificate SHA-256 digest: ([0-9A-Fa-f]{64})\s*$',text,re.MULTILINE)
    if len(pins)!=1:raise ValueError('Exactly one verified certificate is required')
    text=tool([aapt,'dump','badging',str(apk)])
    item=re.search(r"^package: name='([^']+)' versionCode='([0-9]+)'",text,re.MULTILINE)
    if not item or item.group(1)!=APP:raise ValueError('Wrong applicationId')
    return int(item.group(2)),pins[0].upper()


def validate(report,raw,installed,new,room):
    old_code,old_cert=installed;code,cert=new
    if old_cert!=CERT or cert!=CERT:raise ValueError('Installed and new certificates must match the approved original SHA-256')
    if code<=old_code or code>2100000000:raise ValueError('New versionCode must be strictly greater than installed')
    target=report.get('roomVersion')
    allowed={22:(20,21,22),23:(22,),24:(23,24)}
    if target not in allowed or room not in allowed[target] or room>target:
        raise ValueError('Room update requires an explicitly audited pair; no downgrade')
    if target in (23,24):
        product,workflow,floor=('1.0.8',387641531,3000003) if target==23 else ('1.0.9',389125371,3000005)
        if target==24 and room==24:
            product,workflow,floor='1.0.10',0,3000007
        audit=report.get('auditRunId')
        if (old_code<floor or report.get('versionName')!=product or type(audit) is not int or audit<=0 or
                report.get('auditWorkflowId')!=workflow or report.get('auditSourceSha')!=report.get('sourceSha')):
            raise ValueError('Missing exact source migration evidence or wrong product version')
    expected={'applicationId':APP,'versionCode':code,'certificateSha256':CERT,
              'apkSha256':hashlib.sha256(raw).hexdigest(),'installedVersionCode':old_code,'installedRoomVersion':room}
    if any(report.get(k)!=v for k,v in expected.items()):raise ValueError('APK report or installed baseline differs; rebuild with actual inputs')
    if not re.fullmatch(r'[0-9a-f]{40}',str(report.get('sourceSha',''))):raise ValueError('Missing checked source commit')


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk',required=True,type=Path)
    parser.add_argument('--report',required=True,type=Path)
    parser.add_argument('--serial',required=True)
    parser.add_argument('--installed-room-version',required=True,type=int,choices=[20,21,22,23,24])
    parser.add_argument('--adb',default='adb')
    sdk=Path(os.environ.get('ANDROID_HOME',''))/'build-tools/35.0.0'
    parser.add_argument('--aapt2',default=str(sdk/'aapt2'))
    parser.add_argument('--apksigner',default=str(sdk/'apksigner'))
    args=parser.parse_args()
    paths=tool([args.adb,'-s',args.serial,'shell','pm','path',APP]).splitlines()
    bases=[line[len('package:'):] for line in paths if line.startswith('package:') and line.endswith('/base.apk')]
    if len(bases)!=1 or not re.fullmatch(r'/data/app/[A-Za-z0-9_~.+/=:-]+/base\.apk',bases[0]):
        raise ValueError('Cannot safely locate exactly one installed base APK')
    with tempfile.TemporaryDirectory(prefix='main-apk-readonly-') as folder:
        installed=Path(folder)/'installed.apk'
        tool([args.adb,'-s',args.serial,'pull',bases[0],str(installed)])
        os.chmod(installed,0o600)
        old=metadata(installed,args.aapt2,args.apksigner)
        new=metadata(args.apk,args.aapt2,args.apksigner)
        validate(json.loads(args.report.read_text()),args.apk.read_bytes(),old,new,args.installed_room_version)
    print('READ-ONLY UPDATE CHECK PASS: original certificate, main package, strictly higher versionCode and audited Room pair; no phone data changed')

if __name__=='__main__':
    try:main()
    except Exception:raise SystemExit('Update verification failed. Do not install, uninstall or clear data; check package/certificate/version/Room inputs.')
