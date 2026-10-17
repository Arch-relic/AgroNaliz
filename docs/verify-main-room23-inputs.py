"""Read-only APK inspection: production certificates/keys never enter this CI job."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

sdk = Path(os.environ['ANDROID_HOME']) / 'build-tools/35.0.0'


def identity(path, code, name):
    text = subprocess.check_output([str(sdk/'aapt2'), 'dump', 'badging', str(path)], text=True)
    match = re.search(r"^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'", text, re.MULTILINE)
    if not match or match.groups() != ('ru.fo6osik.workjournal', str(code), name):
        raise RuntimeError('Unexpected MAIN APK identity/version')


def certificate(path):
    text = subprocess.check_output([str(sdk/'apksigner'), 'verify', '--print-certs', str(path)], text=True)
    values = re.findall(r'^Signer #\d+ certificate SHA-256 digest: ([a-fA-F0-9]{64})$', text, re.MULTILINE)
    if len(values) != 1:
        raise RuntimeError('Expected one verified CI signer')
    return values[0].upper()


def main():
    old = Path('.main-room22/app/build/outputs/apk/debug/app-debug.apk')
    new = Path('app/build/outputs/apk/debug/app-debug.apk')
    unsigned = Path('app/build/outputs/apk/release/app-release-unsigned.apk')
    identity(old, 3000003, '1.0.7'); identity(new, 3000004, '1.0.8')
    pin = certificate(old)
    if certificate(new) != pin:
        raise RuntimeError('CI fixture certificates differ')
    if pin == '3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C':
        raise RuntimeError('Production certificate must never enter disposable CI fixtures')
    identity(unsigned, 1, '1.0.8')
    result = subprocess.run([str(sdk/'apksigner'), 'verify', str(unsigned)], capture_output=True, text=True)
    if result.returncode == 0 or 'DOES NOT VERIFY' not in result.stderr + result.stdout:
        raise RuntimeError('Expected genuinely unsigned analysis APK, not an installable release')
    Path('main-room23-analysis.json').write_text(json.dumps(dict(
        sourceSha=os.environ['GITHUB_SHA'], applicationId='ru.fo6osik.workjournal',
        oldVersionCode=3000003, fixtureVersionCode=3000004, ciCertificateSha256=pin,
        unsignedAnalysis=dict(versionCode=1, versionName='1.0.8', signed=False,
            apkSha256=hashlib.sha256(unsigned.read_bytes()).hexdigest(), published=False),
        productionReleaseAuthorized=False), indent=2)+'\n')
    print('MAIN fixture identity, common CI certificate, and unsigned analysis APK verified; no APK publication')


if __name__ == '__main__':
    main()
