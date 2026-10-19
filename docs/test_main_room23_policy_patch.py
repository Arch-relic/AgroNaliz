"""Exercise a detached policy patch in /tmp; never modify live workflows or use real keys/network."""
import base64
import contextlib
import copy
import hashlib
import io
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import textwrap
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
PATCH = ROOT/'docs/patches/main-room23-release-policy.patch'
MANIFEST = json.loads((PATCH.with_suffix('.json')).read_text())
STEPS = ['Build future MAIN fixture, tests and unsigned analysis APK',
         'Build exact MAIN1.0.7 Room22 fixture with code3000003',
         'Verify fixture identities and unsigned analysis metadata',
         'Upgrade synthetic MAIN22, compare all columns, restore separately, restart']


def inline(source):
    return textwrap.dedent(source.split("          python3 - <<'PY'\n", 1)[1].split('\n          PY', 1)[0])


class MainRoom23PolicyPatchTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='agro-policy-review-')
        self.addCleanup(self.temp.cleanup)
        self.stage = Path(self.temp.name)
        shutil.copytree(ROOT/'.github/workflows', self.stage/'.github/workflows')
        support = ['app/build.gradle.kts', 'app/src/main/java/ru/fo6osik/workjournal/AppDatabase.kt',
                   'app/src/main/java/ru/fo6osik/workjournal/OptionsActivity.kt',
                   'app/src/main/res/layout/activity_main.xml', 'app/src/main/res/layout/activity_options.xml']
        for name in set(MANIFEST['files']) | set(support):
            dest = self.stage/name; dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT/name, dest)
        # The approved policy's exact hashes remain frozen. Room24 application tests
        # run independently; this sandbox must continue exercising the original Room23 proposal.
        shutil.copyfile(ROOT/'docs/fixtures/room23-app-version.txt', self.stage/'docs/test_app_version.py')
        for target, fixture in [('.github/workflows/main-apk-publish.yml', 'room23-main-apk-publish.yml.txt'),
                                ('docs/test_main_apk_release.py', 'room23-test-main-apk-release.py.txt'),
                                ('docs/main-original-update.md', 'room23-main-original-update.md.txt')]:
            shutil.copyfile(ROOT/'docs/fixtures'/fixture, self.stage/target)
        gradle=self.stage/'app/build.gradle.kts'
        gradle.write_text(gradle.read_text().replace('versionName = "1.0.9"','versionName = "1.0.8"'))
        db = self.stage/'app/src/main/java/ru/fo6osik/workjournal/AppDatabase.kt'
        db.write_text(re.sub(r'version\s*=\s*24', 'version = 23', db.read_text()))
        before = all(hashlib.sha256((self.stage/p).read_bytes()).hexdigest() == v['beforeSha256']
                     for p, v in MANIFEST['files'].items())
        after = all(hashlib.sha256((self.stage/p).read_bytes()).hexdigest() == v['afterSha256']
                    for p, v in MANIFEST['files'].items())
        self.assertTrue(before or after, 'Policy targets drifted; regenerate and review the proposal')
        # Forward apply only in the temporary copy. Also works after a separately approved application.
        if before:
            subprocess.run(['git', 'apply', '--check', str(PATCH)], cwd=self.stage, check=True, capture_output=True)
            subprocess.run(['git', 'apply', str(PATCH)], cwd=self.stage, check=True, capture_output=True)
        else:
            subprocess.run(['git', 'apply', '--reverse', '--check', str(PATCH)], cwd=self.stage, check=True, capture_output=True)
        for p, v in MANIFEST['files'].items():
            self.assertEqual(v['afterSha256'], hashlib.sha256((self.stage/p).read_bytes()).hexdigest())
        self.workflow = (self.stage/'.github/workflows/main-apk-publish.yml').read_text()
        self.preflight = inline(self.workflow)
        self.signer = {'__name__': 'offline_proposed_signer'}
        exec(compile(inline(self.workflow.split('      - name: Sign and verify stable MAIN certificate\n', 1)[1]),
                     'detached-policy-signer', 'exec'), self.signer)
        self.audit_run = dict(id=123, workflow_id=387641531, path='.github/workflows/main-room23-checks.yml',
                        head_sha='a'*40, head_branch='main', event='workflow_dispatch', run_attempt=1,
                        status='completed', conclusion='success', repository=dict(full_name='Arch-relic/AgroNaliz'),
                        head_repository=dict(full_name='Arch-relic/AgroNaliz'), actor=dict(login='Arch-relic'),
                        triggering_actor=dict(login='Arch-relic'))
        self.jobs = dict(total_count=1, jobs=[dict(name='main-room23', status='completed', conclusion='success',
                         steps=[dict(name=n, status='completed', conclusion='success') for n in STEPS])])

    def preflight_run(self, variables=None, previous=(3,), room=23, api_error=None):
        env = dict(GITHUB_RUN_NUMBER='4', INSTALLED_CODE='3000003', INSTALLED_ROOM='22', AUDIT_RUN_ID='123',
                   GITHUB_REPOSITORY='Arch-relic/AgroNaliz', GITHUB_REPOSITORY_OWNER='Arch-relic',
                   GITHUB_SHA='a'*40, GITHUB_REF='refs/heads/main', GITHUB_EVENT_NAME='workflow_dispatch',
                   GITHUB_ACTOR='Arch-relic', GITHUB_RUN_ATTEMPT='1', GH_TOKEN='synthetic',
                   GITHUB_OUTPUT=str(self.stage/'outputs'), GITHUB_ENV=str(self.stage/'env'))
        env.update(variables or {})
        for name in ['outputs', 'env']:
            (self.stage/name).unlink(missing_ok=True)
        (self.stage/'app/src/main/java/ru/fo6osik/workjournal/AppDatabase.kt').write_text(f'version = {room}')
        def response(request, **kwargs):
            url = request.full_url
            if api_error and api_error in url:
                raise OSError('synthetic unavailable API')
            if url.endswith('/actions/runs/123'):
                value = self.audit_run
            elif url.endswith('/actions/runs/123/jobs?per_page=100'):
                value = self.jobs
            elif 'main-apk-publish.yml/runs?' in url:
                value = dict(workflow_runs=[dict(run_number=n) for n in previous])
            else:
                raise AssertionError('Unexpected URL; no real network allowed')
            return io.BytesIO(json.dumps(value).encode())
        cwd = Path.cwd()
        try:
            os.chdir(self.stage)
            with patch.dict(os.environ, env, clear=True), patch('urllib.request.urlopen', side_effect=response):
                exec(compile(self.preflight, 'detached-policy-preflight', 'exec'), {})
        finally:
            os.chdir(cwd)
        return (self.stage/'outputs').read_text()

    def signer_run(self, package='ru.fo6osik.workjournal', version=3000004, name='1.0.8',
                   certificate=None, variables=None, signed_package=None):
        root = self.stage/'sign-run'
        if root.exists(): shutil.rmtree(root)
        (root/'agronaliz-main-input').mkdir(parents=True)
        (root/'agronaliz-main-input/app-debug.apk').write_bytes(b'synthetic-not-an-installable-apk')
        calls = []
        def tool(command):
            calls.append(command)
            if 'badging' in command:
                app = signed_package if str(command[-1]).endswith('verified.apk') and signed_package else package
                return f"package: name='{app}' versionCode='{version}' versionName='{name}'"
            if 'sign' in command:
                Path(command[command.index('--out')+1]).write_bytes(b'synthetic-output')
                return ''
            if 'verify' in command:
                return 'Signer #1 certificate SHA-256 digest: ' + (certificate or 'AB'*32)
            raise AssertionError('Unexpected fake SDK call')
        env = dict(EXPECTED_CERT_SHA256='AB'*32, ANDROID_KEYSTORE_BASE64=base64.b64encode(b'synthetic-key-fixture').decode(),
                   ANDROID_KEYSTORE_PASSWORD='fixture', ANDROID_KEY_ALIAS='fixture', ANDROID_KEY_PASSWORD='',
                   ANDROID_HOME='/synthetic-sdk', RUNNER_TEMP=str(root), MAIN_VERSION_CODE=str(version),
                   INSTALLED_CODE='3000003', INSTALLED_ROOM='22', MAIN_ROOM_VERSION='23',
                   MAIN_MIGRATION_AUDIT_RUN_ID='123', APK_SOURCE_SHA='a'*40, GITHUB_OUTPUT=str(root/'outputs'))
        env.update(variables or {})
        with patch.dict(os.environ, env, clear=True), patch.dict(self.signer, {'tool': tool}), contextlib.redirect_stdout(io.StringIO()):
            self.signer['main']()
        return json.loads((root/'agronaliz-main-output/verification.json').read_text())

    def test_matching_owner_main_audit_enables_exact_22_to23(self):
        output = self.preflight_run()
        self.assertIn('version_code=3000004', output)
        self.assertIn('room_version=23', output)
        self.assertIn('audit_run_id=123', output)
        info = self.signer_run()
        self.assertEqual('ru.fo6osik.workjournal', info['applicationId'])
        self.assertEqual('1.0.8', info['versionName'])
        self.assertEqual(123, info['auditRunId'])
        self.assertEqual('a'*40, info['auditSourceSha'])
        self.assertEqual('AB'*32, info['certificateSha256'])

    def test_audit_identity_status_and_actor_mismatches_rejected(self):
        valid = copy.deepcopy(self.audit_run)
        changes = [('id',124),('workflow_id',1),('path','wrong.yml'),('head_sha','b'*40),
                   ('head_branch','feature/harvest-1-0-8'),('event','pull_request'),('run_attempt',2),
                   ('status','in_progress'),('conclusion','failure'),('conclusion','skipped'),
                   ('repository',dict(full_name='fork/repo')),('head_repository',dict(full_name='fork/repo')),
                   ('actor',dict(login='outsider')),('triggering_actor',dict(login='outsider'))]
        for key, value in changes:
            with self.subTest(key=key, value=value):
                self.audit_run = dict(copy.deepcopy(valid), **{key:value})
                with self.assertRaises(SystemExit): self.preflight_run()
                self.assertFalse((self.stage/'outputs').exists())

    def test_skipped_missing_failed_ambiguous_or_truncated_job_rejected(self):
        good = copy.deepcopy(self.jobs)
        variants = [dict(total_count=0,jobs=[]),dict(total_count=2,jobs=good['jobs']),
                    dict(total_count=2,jobs=good['jobs']*2)]
        for field, value in [('status','in_progress'),('conclusion','skipped'),('steps',[])]:
            bad=copy.deepcopy(good);bad['jobs'][0][field]=value;variants.append(bad)
        for index in range(len(STEPS)):
            bad=copy.deepcopy(good);bad['jobs'][0]['steps'][index]['conclusion']='skipped';variants.append(bad)
        for value in variants:
            with self.subTest(value=value):
                self.jobs=value
                with self.assertRaises(SystemExit):self.preflight_run()

    def test_api_failures_block_before_build_output(self):
        for endpoint in ['/actions/runs/123', '/jobs?', 'main-apk-publish.yml/runs?']:
            with self.subTest(endpoint=endpoint), self.assertRaises(SystemExit): self.preflight_run(api_error=endpoint)

    def test_missing_audit_unsupported_schema_and_non_increasing_code_blocked(self):
        for variables in [dict(AUDIT_RUN_ID=''),dict(AUDIT_RUN_ID='0'),dict(AUDIT_RUN_ID='x'),
                          dict(INSTALLED_ROOM='20'),dict(INSTALLED_ROOM='21'),dict(INSTALLED_ROOM='23'),
                          dict(INSTALLED_CODE='53'),dict(INSTALLED_CODE='3000004'),
                          dict(GITHUB_RUN_NUMBER='2100000000')]:
            with self.subTest(variables=variables), self.assertRaises(SystemExit): self.preflight_run(variables)
        with self.assertRaises(SystemExit):self.preflight_run(room=24)
        for history in [(4,), (5,)]:
            with self.subTest(history=history), self.assertRaises(SystemExit):self.preflight_run(previous=history)

    def test_dispatch_trust_boundary_and_wrong_product_version_blocked(self):
        for variables in [dict(GITHUB_REPOSITORY='fork/repo'),dict(GITHUB_REPOSITORY_OWNER='other'),
                          dict(GITHUB_REF='refs/heads/feature/harvest-1-0-8'),dict(GITHUB_EVENT_NAME='push'),
                          dict(GITHUB_ACTOR='other'),dict(GITHUB_RUN_ATTEMPT='2')]:
            with self.subTest(variables=variables), self.assertRaises(SystemExit):self.preflight_run(variables)
        p=self.stage/'app/build.gradle.kts';p.write_text(p.read_text().replace('versionName = "1.0.8"','versionName = "1.0.7"'))
        with self.assertRaises(SystemExit):self.preflight_run()

    def test_legacy_room22_branch_keeps_sequence_and_supported_pairs(self):
        for installed in ['20','21','22']:
            self.assertIn('version_code=3000004',self.preflight_run(dict(INSTALLED_ROOM=installed,INSTALLED_CODE='53',AUDIT_RUN_ID=''),room=22))
        with self.assertRaises(SystemExit):self.preflight_run(dict(INSTALLED_ROOM='23'),room=22)

    def test_signer_cannot_publish_test_or_changed_package_or_wrong_certificate(self):
        for kwargs in [dict(package='ru.fo6osik.workjournal.debugtest'),dict(package='other'),
                       dict(signed_package='ru.fo6osik.workjournal.debugtest'),dict(certificate='00'*32),
                       dict(name='1.0.7'),dict(version=3000003),dict(variables=dict(MAIN_MIGRATION_AUDIT_RUN_ID='')),
                       dict(variables=dict(INSTALLED_ROOM='23'))]:
            with self.subTest(kwargs=kwargs), self.assertRaises(self.signer['SigningError']):self.signer_run(**kwargs)
            self.assertFalse((self.stage/'sign-run/agronaliz-main-output').exists())
            self.assertEqual([],list((self.stage/'sign-run').glob('agronaliz-main-key-*')))

    def test_original_guards_and_secret_isolation_unchanged(self):
        guard="github.ref == 'refs/heads/main' && github.actor == github.repository_owner && github.run_attempt == 1"
        self.assertEqual(2,self.workflow.count(guard))
        self.assertNotIn('pull_request:',self.workflow);self.assertNotIn('pull_request_target:',self.workflow)
        self.assertNotIn('secrets.TEST_',self.workflow)
        build,sign=self.workflow.split('\n  sign:\n',1)
        self.assertNotIn('${{ secrets.',build);self.assertNotIn('actions/checkout',sign);self.assertNotIn('./gradlew',sign)
        self.assertIn('3FE57967C15F81EE39EA2B9B88DD96A7506C1B5B9032606ECBEA04BBDB15EE3C',sign)
        self.assertIn('auditSourceSha',sign);self.assertIn('Remove internal unsigned transport artifact',sign)
        for name in STEPS:self.assertIn(name,(ROOT/'.github/workflows/main-room23-checks.yml').read_text())

    def test_patched_existing_test_suites_pass_without_real_apks(self):
        for pattern in ['test_main_apk_release.py','test_app_version.py']:
            result=subprocess.run([sys.executable,'-m','unittest','discover','-s','docs','-p',pattern,'-q'],
                                  cwd=self.stage,capture_output=True,text=True)
            self.assertEqual(0,result.returncode,result.stdout+result.stderr)
            print('TEMP PATCH '+pattern+': '+result.stderr.strip().splitlines()[-1])


if __name__ == '__main__':
    unittest.main()
