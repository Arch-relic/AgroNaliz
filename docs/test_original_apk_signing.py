"""Test the exact inline signing program; production secrets are never used by tests."""
import contextlib
import hashlib
import io
import json
import os
from pathlib import Path
import shutil
import stat
import subprocess
import tempfile
import textwrap
import unittest
from unittest.mock import patch
import zipfile

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = (ROOT / '.github/workflows/android-pr-checks.yml').read_text()
STEP = WORKFLOW.split('      - name: Sign and verify original AndroidIDE certificate\n', 1)[1]
CODE = textwrap.dedent(STEP.split("          python3 - <<'PY'\n", 1)[1].split('\n          PY', 1)[0])
PROGRAM = {'__name__': 'signing_tests'}
exec(compile(CODE, 'inline-signing-workflow', 'exec'), PROGRAM)
ERROR = PROGRAM['SigningError']
VALID = 'AB' * 32


class SigningVerificationTest(unittest.TestCase):
    def test_exact_public_pin(self):
        self.assertEqual(VALID, PROGRAM['normalize_certificate'](VALID.lower()))

    def test_colon_separated_public_pin(self):
        self.assertEqual(VALID, PROGRAM['normalize_certificate'](':'.join(['AB'] * 32)))

    def test_incomplete_62_character_pin_is_rejected(self):
        with self.assertRaises(ERROR): PROGRAM['normalize_certificate']('AB' * 31)

    def test_invalid_pin_values(self):
        for value in ['', 'G' * 64, 'A' * 63, 'A' * 65, 'A\n' * 32]:
            with self.subTest(value_length=len(value)), self.assertRaises(ERROR):
                PROGRAM['normalize_certificate'](value)

    def test_single_verified_certificate(self):
        self.assertEqual(VALID, PROGRAM['certificate_digest']('Signer #1 certificate SHA-256 digest: ' + VALID.lower()))

    def test_no_certificate_is_rejected(self):
        with self.assertRaises(ERROR): PROGRAM['certificate_digest']('not verified')

    def test_multiple_certificates_are_rejected(self):
        text = '\n'.join('Signer #{} certificate SHA-256 digest: {}'.format(i, VALID) for i in [1, 2])
        with self.assertRaises(ERROR): PROGRAM['certificate_digest'](text)

    def test_main_id_and_version(self):
        self.assertEqual(('ru.fo6osik.workjournal', 49), PROGRAM['apk_identity']("package: name='ru.fo6osik.workjournal' versionCode='49'"))

    def test_test_or_wrong_package_is_rejected(self):
        for name in ['ru.fo6osik.workjournal.debugtest', 'other.package']:
            with self.assertRaises(ERROR): PROGRAM['apk_identity']("package: name='{}' versionCode='49'".format(name))

    def test_invalid_pin_prevents_any_key_or_tool_access(self):
        with patch.dict(os.environ, {'EXPECTED_CERT_SHA256': 'AB' * 31}, clear=True), patch.dict(PROGRAM, {'tool': lambda _: self.fail('tool accessed')}):
            with self.assertRaises(ERROR): PROGRAM['main']()

    def test_missing_secrets_fail_closed(self):
        with patch.dict(os.environ, {'EXPECTED_CERT_SHA256': VALID}, clear=True):
            with self.assertRaises(ERROR): PROGRAM['main']()

    def test_tool_error_does_not_expose_credentials_or_alias(self):
        result = subprocess.CompletedProcess([], 1, 'fixture-private-alias', 'fixture-password')
        with patch('subprocess.run', return_value=result), self.assertRaises(ERROR) as caught:
            PROGRAM['tool'](['apksigner'])
        self.assertNotIn('fixture-private-alias', str(caught.exception))
        self.assertNotIn('fixture-password', str(caught.exception))

    def test_signing_job_has_no_checkout_or_gradle(self):
        job = WORKFLOW.split('\n  original-signature:\n', 1)[1]
        self.assertNotIn('uses: actions/checkout', job)
        self.assertNotIn('./gradlew', job)
        self.assertIn("github.event.pull_request.head.repo.full_name == github.repository", job)
        self.assertIn("github.actor == github.repository_owner", job)
        self.assertIn("github.event.pull_request.user.login == github.repository_owner", job)
        self.assertIn("github.event.pull_request.head.ref == 'feature/original-key-signing'", job)
        self.assertIn("github.ref == 'refs/heads/main'", job)

    def test_secrets_are_only_in_isolated_signing_step(self):
        build = WORKFLOW.split('\n  original-signature:\n', 1)[0]
        self.assertNotIn('${{ secrets.', build)
        self.assertNotIn('pull_request_target:', WORKFLOW)
        self.assertIn("'--ks-pass', 'env:ANDROID_KEYSTORE_PASSWORD'", CODE)
        self.assertIn("'--key-pass', 'env:' + key_password_env", CODE)
        self.assertIn("'ANDROID_KEY_PASSWORD' if os.environ.get('ANDROID_KEY_PASSWORD') else 'ANDROID_KEYSTORE_PASSWORD'", CODE)
        self.assertIn("'--v1-signer-name', 'ORIGINAL'", CODE)
        self.assertIn('0o600', CODE)
        self.assertIn('0o700', CODE)

    def test_artifact_has_no_secret_or_private_directory(self):
        upload = STEP.split('      - name: Publish only', 1)[1].split('      - name: Remove', 1)[0]
        self.assertNotIn('if: always()', upload)
        self.assertNotIn('agronaliz-key-', upload)
        self.assertIn('app-original-signed.apk', upload)
        self.assertIn('verification.json', upload)
        self.assertNotIn('original.keystore', upload)

    def test_real_sdk_resigning_and_mismatch_cleanup(self):
        # This integration runs after assembleDebug in Actions. Its keystore is temporary,
        # generated from synthetic fixture credentials, and never saved to the repository.
        source = ROOT / 'app/build/outputs/apk/debug/app-debug.apk'
        sdk = Path(os.environ.get('ANDROID_HOME', '/workspace/android-sdk'))
        tools = sdk / 'build-tools/35.0.0'
        if not source.is_file() or not (tools / 'apksigner').is_file() or not shutil.which('keytool'):
            self.skipTest('Built debug APK and Android SDK tools required for real signing fixture')
        with tempfile.TemporaryDirectory(prefix='agronaliz-fixture-') as folder:
            root = Path(folder)
            key = root / 'fixture.jks'
            fixture_password = 'fixture-password-not-a-production-secret'
            env = dict(os.environ, FIXTURE_PASSWORD=fixture_password)
            subprocess.run(['keytool', '-genkeypair', '-keystore', str(key), '-alias', 'fixture-private-alias',
                            '-storepass:env', 'FIXTURE_PASSWORD', '-keypass:env', 'FIXTURE_PASSWORD',
                            '-dname', 'CN=SigningFixture', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '1'],
                           env=env, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            certificate = subprocess.check_output(['keytool', '-exportcert', '-keystore', str(key),
                                                   '-alias', 'fixture-private-alias', '-storepass:env', 'FIXTURE_PASSWORD'], env=env, stderr=subprocess.PIPE)
            digest = hashlib.sha256(certificate).hexdigest().upper()
            import base64
            input_dir = root / 'agronaliz-original-input';input_dir.mkdir()
            shutil.copyfile(source, input_dir / 'app-debug.apk')
            before = hashlib.sha256((input_dir / 'app-debug.apk').read_bytes()).hexdigest()
            badging = subprocess.check_output([str(tools / 'aapt2'), 'dump', 'badging', str(source)], text=True)
            _, version = PROGRAM['apk_identity'](badging)
            variables = dict(ANDROID_HOME=str(sdk), RUNNER_TEMP=str(root), EXPECTED_CERT_SHA256=digest,
                             ANDROID_KEYSTORE_BASE64=base64.b64encode(key.read_bytes()).decode(),
                             ANDROID_KEYSTORE_PASSWORD=fixture_password, ANDROID_KEY_PASSWORD=fixture_password,
                             ANDROID_KEY_ALIAS='fixture-private-alias', APK_VERSION_CODE=str(version),
                             APK_SOURCE_SHA='a' * 40, GITHUB_OUTPUT=str(root / 'outputs'))
            with patch.dict(os.environ, variables), contextlib.redirect_stdout(io.StringIO()): PROGRAM['main']()
            output = root / 'agronaliz-original-output'
            report = json.loads((output / 'verification.json').read_text())
            self.assertEqual(digest, report['certificateSha256'])
            self.assertEqual(version, report['versionCode'])
            self.assertEqual(before, hashlib.sha256((input_dir / 'app-debug.apk').read_bytes()).hexdigest())
            self.assertEqual([], list(root.glob('agronaliz-key-*')))
            with zipfile.ZipFile(output / 'app-original-signed.apk') as archive:
                self.assertIn('META-INF/ORIGINAL.SF', archive.namelist())
                self.assertFalse(any('fixture-private-alias' in name.lower() for name in archive.namelist()))
            shutil.rmtree(output)
            # Default key-password behavior still authenticates/decrypts the private key.
            variables['ANDROID_KEY_PASSWORD'] = ''
            with patch.dict(os.environ, variables), contextlib.redirect_stdout(io.StringIO()): PROGRAM['main']()
            self.assertEqual(digest, json.loads((output / 'verification.json').read_text())['certificateSha256'])
            shutil.rmtree(output)
            # An explicitly supplied wrong password never silently falls back.
            variables['ANDROID_KEY_PASSWORD'] = 'intentionally-wrong-fixture-password'
            with patch.dict(os.environ, variables), self.assertRaises(ERROR): PROGRAM['main']()
            self.assertFalse(output.exists())
            self.assertEqual([], list(root.glob('agronaliz-key-*')))
            variables['ANDROID_KEY_PASSWORD'] = fixture_password
            variables['EXPECTED_CERT_SHA256'] = '0' * 64
            with patch.dict(os.environ, variables), self.assertRaises(ERROR): PROGRAM['main']()
            self.assertFalse(output.exists())
            self.assertEqual([], list(root.glob('agronaliz-key-*')))


if __name__ == '__main__':
    unittest.main()
