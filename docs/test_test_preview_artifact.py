"""Validate the exact final publication gate; only public synthetic metadata is used."""
import copy
from pathlib import Path
import textwrap
import unittest

WORKFLOW = (Path(__file__).resolve().parents[1] / '.github/workflows/test-apk-publish.yml').read_text()
STEP = WORKFLOW.split('      - name: Verify PR33 signed APK against last stable TEST\n', 1)[1]
CODE = textwrap.dedent(STEP.split("          python3 - <<'PY'\n", 1)[1].split('\n          PY', 1)[0])
PROGRAM = {'__name__': 'preview_metadata_test'}
exec(compile(CODE, 'preview-publication-gate', 'exec'), PROGRAM)


class PreviewArtifactTest(unittest.TestCase):
    def report(self):
        return dict(applicationId='ru.fo6osik.workjournal.debugtest', versionCode=2000008,
                    certificateSha256=PROGRAM['LAST_CERT'], roomVersion=23, sourceSha='a'*40)

    def badging(self, package='ru.fo6osik.workjournal.debugtest', code=2000008, name='1.0.8'):
        return f"package: name='{package}' versionCode='{code}' versionName='{name}' platformBuildVersionName='15'\n"

    def validate(self, report=None, badging=None, code=2000008):
        return PROGRAM['validate'](self.report() if report is None else report,
                                   self.badging() if badging is None else badging, code, 'a'*40)

    def test_valid_metadata_matches_provided_phone_screenshots(self):
        result = self.validate()
        self.assertEqual('1.0.8', result['versionName'])
        self.assertEqual(2000007, result['comparedStableVersionCode'])
        self.assertEqual('MATCHES_PROVIDED_SCREENSHOTS', result['installedPhoneCompatibility'])
        self.assertEqual(2000005, result['installedPhoneVersionCode'])
        self.assertEqual(PROGRAM['LAST_CERT'], result['installedPhoneCertificateSha256'])

    def test_main_and_wrong_versions_are_not_published(self):
        for badging in [self.badging(package='ru.fo6osik.workjournal'), self.badging(name='1.0.7'),
                        self.badging(code=2000009), 'not an APK']:
            with self.assertRaises(ValueError): self.validate(badging=badging)

    def test_non_increasing_and_out_of_range_codes_blocked(self):
        for code in [2000007, 1, 2100000001]:
            report = self.report(); report['versionCode'] = code
            with self.assertRaises(ValueError): self.validate(report, self.badging(code=code), code)

    def test_different_certificate_blocks_publication(self):
        report = self.report(); report['certificateSha256'] = 'AB'*32
        with self.assertRaises(ValueError): self.validate(report)

    def test_different_source_schema_or_report_identity_blocked(self):
        for name, value in [('sourceSha', 'b'*40), ('roomVersion', 22), ('versionCode', 2000009),
                            ('applicationId', 'ru.fo6osik.workjournal')]:
            report = copy.deepcopy(self.report()); report[name] = value
            with self.assertRaises(ValueError): self.validate(report)

    def test_gate_runs_after_signing_before_upload(self):
        self.assertLess(WORKFLOW.index('Sign and verify stable TEST certificate'),
                        WORKFLOW.index('Verify PR33 signed APK against last stable TEST'))
        self.assertLess(WORKFLOW.index('Verify PR33 signed APK against last stable TEST'),
                        WORKFLOW.index('Publish verified TEST APK'))
        self.assertNotIn('secrets.', STEP.split('      - name: Publish verified TEST APK')[0])


if __name__ == '__main__':
    unittest.main()
