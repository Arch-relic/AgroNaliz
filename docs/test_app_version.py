"""Check public version metadata and execute unchanged release preflights with fake HTTP."""
import io
import json
import os
from pathlib import Path
import re
import tempfile
import textwrap
import unittest
from unittest.mock import patch
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


def preflight(workflow, number, installed, previous=()):
    source = (ROOT / '.github/workflows' / workflow).read_text()
    code = textwrap.dedent(source.split("          python3 - <<'PY'\n", 1)[1].split('\n          PY', 1)[0])
    with tempfile.TemporaryDirectory() as folder:
        env = dict(GITHUB_RUN_NUMBER=str(number), INSTALLED_CODE=str(installed), INSTALLED_ROOM='22',
                   GITHUB_REPOSITORY='synthetic/repository', GITHUB_SHA='a' * 40, GH_TOKEN='synthetic',
                   GITHUB_OUTPUT=folder + '/output', GITHUB_ENV=folder + '/env')
        response = io.BytesIO(json.dumps({'workflow_runs': [{'run_number': n} for n in previous]}).encode())
        with patch.dict(os.environ, env), patch('urllib.request.urlopen', return_value=response):
            exec(compile(code, workflow + ':preflight', 'exec'), {'__name__': 'version_policy_test'})
        return int(re.search(r'version_code=(\d+)', Path(env['GITHUB_OUTPUT']).read_text()).group(1))


class AppVersionTest(unittest.TestCase):
    def test_single_three_component_version_for_all_variants(self):
        text = (ROOT / 'app/build.gradle.kts').read_text()
        versions = re.findall(r'versionName\s*=\s*"([^"]+)"', text)
        self.assertEqual(['1.0.8'], versions)
        self.assertRegex(versions[0], r'^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$')
        self.assertNotIn('versionNameSuffix', text)

    def test_main_release_codes_increase_without_reset(self):
        # Exercise code ordering against the audited Room22 fixture. Production Room23
        # must remain blocked by the unchanged MAIN release gate until a separate audit.
        original = Path.read_text
        def audited(path, *args, **kwargs):
            if path.name == 'AppDatabase.kt': return 'version = 22'
            return original(path, *args, **kwargs)
        with patch('pathlib.Path.read_text', audited):
            self.assertEqual(3000005, preflight('main-apk-publish.yml', 5, 53))
            self.assertEqual(3000006, preflight('main-apk-publish.yml', 6, 3000005, [5]))

    def test_room23_main_publication_remains_blocked(self):
        with self.assertRaisesRegex(SystemExit, 'separate verified migration audit'):
            preflight('main-apk-publish.yml', 5, 53)

    def test_test_release_codes_increase_without_reset(self):
        self.assertEqual(2000005, preflight('test-apk-publish.yml', 5, 53))
        self.assertEqual(2000006, preflight('test-apk-publish.yml', 6, 2000005, [5]))

    def test_equal_or_lower_codes_and_old_runs_are_rejected(self):
        for workflow, base in [('main-apk-publish.yml', 3000000), ('test-apk-publish.yml', 2000000)]:
            for installed, previous in [(base + 5, []), (base + 6, []), (53, [5]), (53, [6])]:
                with self.subTest(workflow=workflow, installed=installed, previous=previous):
                    with self.assertRaises(SystemExit): preflight(workflow, 5, installed, previous)

    def test_room_23_and_dynamic_label(self):
        self.assertRegex((ROOT / 'app/src/main/java/ru/fo6osik/workjournal/AppDatabase.kt').read_text(), r'version\s*=\s*23\s*,')
        root = ET.parse(ROOT / 'app/src/main/res/layout/activity_main.xml').getroot()
        self.assertEqual('ScrollView', root.tag)
        android = '{http://schemas.android.com/apk/res/android}'
        label = next(x for x in root.iter('TextView') if x.get(android + 'id') == '@+id/textAppVersion')
        self.assertIsNone(label.get(android + 'text'))
        self.assertEqual('center', label.get(android + 'gravity'))
        self.assertEqual('12sp', label.get(android + 'textSize'))
        source = (ROOT / 'app/src/main/java/ru/fo6osik/workjournal/MainActivity.kt').read_text()
        self.assertIn('BuildConfig.VERSION_NAME', source)
        self.assertIn('BuildConfig.BUILD_TYPE == "debugTest"', source)


if __name__ == '__main__':
    unittest.main()
