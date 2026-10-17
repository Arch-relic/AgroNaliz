"""Tests the exact secret-isolated inline TEST signer; only synthetic fixture keys are used."""
import base64
import contextlib
import hashlib
import io
import json
import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import tempfile
import textwrap
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = (ROOT / '.github/workflows/test-apk-publish.yml').read_text()
SIGN_STEP = WORKFLOW.split('      - name: Sign and verify stable TEST certificate\n',1)[1]
CODE = textwrap.dedent(SIGN_STEP.split("          python3 - <<'PY'\n",1)[1].split('\n          PY',1)[0])
PREFLIGHT = textwrap.dedent(WORKFLOW.split("          python3 - <<'PY'\n",1)[1].split('\n          PY',1)[0])
PROGRAM = {'__name__':'test_signing_program'}
exec(compile(CODE,'inline-test-signer','exec'),PROGRAM)
ERROR = PROGRAM['SigningError']


def sdk():
    return Path(os.environ.get('ANDROID_HOME','/workspace/android-sdk')) / 'build-tools/35.0.0'


def fixture_key(folder, name):
    key = Path(folder) / (name + '.p12')
    variables = dict(os.environ, FIXTURE_PASSWORD='synthetic-password')
    subprocess.run(['keytool','-genkeypair','-keystore',str(key),'-alias','fixture-alias',
                    '-storepass:env','FIXTURE_PASSWORD','-keypass:env','FIXTURE_PASSWORD',
                    '-dname','CN=TestSigningFixture','-keyalg','RSA','-keysize','2048','-validity','1'],
                   env=variables,check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    certificate = subprocess.check_output(['keytool','-exportcert','-keystore',str(key),'-alias','fixture-alias',
                                          '-storepass:env','FIXTURE_PASSWORD'],env=variables,stderr=subprocess.PIPE)
    os.chmod(key,0o600)
    return key,hashlib.sha256(certificate).hexdigest().upper()


def sign_fixture(source, folder, key, pin):
    root = Path(folder)
    input_dir = root / 'agronaliz-test-input'; input_dir.mkdir()
    shutil.copyfile(source,input_dir / 'app-debugTest.apk')
    identity = PROGRAM['apk_identity'](subprocess.check_output([str(sdk()/'aapt2'),'dump','badging',str(source)],text=True))
    variables=dict(ANDROID_HOME=str(sdk().parents[1]),RUNNER_TEMP=str(root),EXPECTED_CERT_SHA256=pin,
                   TEST_KEYSTORE_BASE64=base64.b64encode(Path(key).read_bytes()).decode(),
                   TEST_KEYSTORE_PASSWORD='synthetic-password',TEST_KEY_ALIAS='fixture-alias',TEST_KEY_PASSWORD='',
                   TEST_VERSION_CODE=str(identity[1]),TEST_ROOM_VERSION=re.search(r'version\s*=\s*(\d+)',(ROOT/'app/src/main/java/ru/fo6osik/workjournal/AppDatabase.kt').read_text()).group(1),APK_SOURCE_SHA='a'*40,GITHUB_OUTPUT=str(root/'outputs'))
    with patch.dict(os.environ,variables),contextlib.redirect_stdout(io.StringIO()):PROGRAM['main']()
    return root/'agronaliz-test-output/app-TEST-stable.apk'


class TestApkSigning(unittest.TestCase):
    def test_pin_accepts_exact_and_colon_public_sha(self):
        self.assertEqual('AB'*32,PROGRAM['normalize_certificate'](':'.join(['ab']*32)))
    def test_pin_rejects_incomplete_and_invalid_values(self):
        for value in ['','A'*62,'A'*65,'G'*64]:
            with self.assertRaises(ERROR):PROGRAM['normalize_certificate'](value)
    def test_only_test_id_and_valid_version_accepted(self):
        self.assertEqual(('ru.fo6osik.workjournal.debugtest',9),PROGRAM['apk_identity']("package: name='ru.fo6osik.workjournal.debugtest' versionCode='9'"))
        for name,code in [('ru.fo6osik.workjournal',1),('ru.fo6osik.workjournal.debugtest',0),('other',1)]:
            with self.assertRaises(ERROR):PROGRAM['apk_identity']("package: name='{}' versionCode='{}'".format(name,code))
    def test_single_certificate_required(self):
        one='Signer #1 certificate SHA-256 digest: '+ 'ab'*32
        self.assertEqual('AB'*32,PROGRAM['certificate_digest'](one))
        for value in ['',one+'\n'+one]:
            with self.assertRaises(ERROR):PROGRAM['certificate_digest'](value)
    def test_missing_pin_prevents_key_access(self):
        with patch.dict(os.environ,{'EXPECTED_CERT_SHA256':''},clear=True),patch.dict(PROGRAM,{'tool':lambda _:self.fail('tool called')}):
            with self.assertRaises(ERROR):PROGRAM['main']()
    def test_missing_secrets_fail_closed(self):
        with patch.dict(os.environ,{'EXPECTED_CERT_SHA256':'AB'*32},clear=True):
            with self.assertRaises(ERROR):PROGRAM['main']()
    def test_tool_errors_suppress_password_and_alias(self):
        with patch('subprocess.run',return_value=subprocess.CompletedProcess([],1,'private-alias','private-password')):
            with self.assertRaises(ERROR) as caught:PROGRAM['tool'](['apksigner'])
            self.assertNotIn('private-alias',str(caught.exception));self.assertNotIn('private-password',str(caught.exception))
    def test_only_owner_approved_source_events_can_use_secrets(self):
        from types import SimpleNamespace
        self.assertNotIn('pull_request:',WORKFLOW);self.assertNotIn('pull_request_target:',WORKFLOW)
        gates=re.findall(r'^    if: (.+)$',WORKFLOW,re.MULTILINE)
        self.assertEqual(2,len(gates));self.assertEqual(gates[0],gates[1])
        code=gates[0].replace('&&',' and ').replace('||',' or ')
        for event,ref,actor,attempt,allowed in [
            ('workflow_dispatch','refs/heads/main','owner',1,True),
            ('push','refs/heads/test-artifact/1.0.8-pr33','owner',1,True),
            ('push','refs/heads/main','owner',1,False),
            ('pull_request','refs/heads/test-artifact/1.0.8-pr33','owner',1,False),
            ('push','refs/heads/feature/harvest-1-0-8','owner',1,False),
            ('workflow_dispatch','refs/heads/test-artifact/1.0.8-pr33','owner',1,False),
            ('push','refs/heads/test-artifact/1.0.8-pr33','contributor',1,False),
            ('push','refs/heads/test-artifact/1.0.8-pr33','owner',2,False),
        ]:
            github=SimpleNamespace(event_name=event,ref=ref,actor=actor,repository_owner='owner',run_attempt=attempt)
            self.assertEqual(allowed,eval(code,{'github':github}), (event,ref,actor,attempt))
        self.assertIn("paths: ['.github/workflows/test-apk-publish.yml']",WORKFLOW)
        build,sign=WORKFLOW.split('\n  sign:\n',1)
        self.assertNotIn('${{ secrets.',build);self.assertNotIn('actions/checkout',sign);self.assertNotIn('./gradlew',sign)
        self.assertNotIn('secrets.ANDROID_',WORKFLOW)
    def test_private_key_not_in_published_files(self):
        upload=SIGN_STEP.split('      - name: Publish verified TEST APK',1)[1].split('      - name: Remove temporary',1)[0]
        self.assertIn('app-TEST-stable.apk',upload);self.assertIn('verification.json',upload)
        self.assertNotIn('keystore',upload);self.assertNotIn('if: always()',upload)
        self.assertIn('0o600',CODE);self.assertIn('0o700',CODE)
        self.assertIn("'--v1-signer-name', 'TEST'",CODE)
    def preflight(self, number=10, floor='1', room='20', previous=(), expect_failure=False):
        with tempfile.TemporaryDirectory() as folder:
            variables=dict(GITHUB_RUN_NUMBER=str(number),INSTALLED_CODE=floor,INSTALLED_ROOM=room,
                           GITHUB_REPOSITORY='Arch-relic/AgroNaliz',GH_TOKEN='fixture',GITHUB_SHA='a'*40,
                           GITHUB_OUTPUT=folder+'/outputs',GITHUB_ENV=folder+'/env')
            response=io.BytesIO(json.dumps({'workflow_runs':[{'run_number':n} for n in previous]}).encode())
            with patch.dict(os.environ,variables,clear=True),patch('urllib.request.urlopen',return_value=response):
                if expect_failure:
                    with self.assertRaises(SystemExit):exec(compile(PREFLIGHT,'test-release-preflight','exec'),{})
                    return
                exec(compile(PREFLIGHT,'test-release-preflight','exec'),{})
            return Path(folder+'/outputs').read_text()
    def test_version_increases_per_new_run(self):
        self.assertIn('version_code=2000010',self.preflight(10,previous=[9]))
        self.assertIn('version_code=2000011',self.preflight(11,previous=[10]))
    def test_version_must_exceed_installed_and_android_bounds(self):
        self.preflight(10,floor='2000010',expect_failure=True)
        self.preflight(2100000000,expect_failure=True)
    def test_out_of_order_publication_blocked(self):self.preflight(10,previous=[11],expect_failure=True)
    def test_database_downgrade_blocked(self):self.preflight(room='99999',expect_failure=True)
    def test_pr_does_not_publish_ephemeral_test_apk(self):
        checks=(ROOT/'.github/workflows/android-pr-checks.yml').read_text()
        self.assertNotIn('name: Upload isolated TEST APK',checks)
        self.assertIn('name: Upload debug APK',checks)
        self.assertIn('source',WORKFLOW)
    def test_real_sdk_signer_and_mismatch_cleanup(self):
        source=ROOT/'app/build/outputs/apk/debugTest/app-debugTest.apk'
        if not source.is_file() or not (sdk()/'apksigner').is_file() or not shutil.which('keytool'):
            self.skipTest('Built TEST APK and SDK required; Actions executes after build')
        main_apk=ROOT/'app/build/outputs/apk/debug/app-debug.apk'
        if main_apk.exists():
            badging=subprocess.check_output([str(sdk()/'aapt2'),'dump','badging',str(main_apk)],text=True)
            self.assertRegex(badging,r"package: name='ru.fo6osik.workjournal' versionCode='1'")
        identity=PROGRAM['apk_identity'](subprocess.check_output([str(sdk()/'aapt2'),'dump','badging',str(source)],text=True))
        if os.environ.get('TEST_VERSION_CODE'):
            self.assertEqual(int(os.environ['TEST_VERSION_CODE']),identity[1])
        before=hashlib.sha256(source.read_bytes()).hexdigest()
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);key,pin=fixture_key(root,'fixture')
            output=sign_fixture(source,root,key,pin)
            info=json.loads((output.parent/'verification.json').read_text())
            self.assertEqual(pin,info['certificateSha256'])
            self.assertEqual('ru.fo6osik.workjournal.debugtest',info['applicationId'])
            self.assertEqual([],list(root.glob('agronaliz-test-key-*')))
            shutil.rmtree(output.parent);shutil.rmtree(root/'agronaliz-test-input')
            with self.assertRaises(ERROR):sign_fixture(source,root,key,'0'*64)
            self.assertFalse(output.parent.exists());self.assertEqual([],list(root.glob('agronaliz-test-key-*')))
        self.assertEqual(before,hashlib.sha256(source.read_bytes()).hexdigest())
