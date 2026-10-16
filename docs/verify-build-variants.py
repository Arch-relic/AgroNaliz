"""Verify actual built APK identities and merged provider/backup declarations."""
import os
import re
import subprocess
from pathlib import Path
from xml.etree import ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
SDK = Path(os.environ.get("ANDROID_HOME", os.environ.get("ANDROID_SDK_ROOT", "")))
AAPT = SDK / "build-tools/35.0.0/aapt2"


def verify(variant, expected_id, expected_label=None):
    apk = Path(f"app/build/outputs/apk/{variant}/app-{variant}.apk")
    badging = subprocess.check_output([str(AAPT), "dump", "badging", str(apk)], text=True)
    actual_id = re.search(r"^package: name='([^']+)'", badging, re.MULTILINE).group(1)
    assert actual_id == expected_id, (variant, actual_id)
    actual_name = re.search(r"versionName='([^']+)'", badging).group(1)
    declared = re.search(r'versionName\s*=\s*"([^"\n]+)"', Path("app/build.gradle.kts").read_text()).group(1)
    assert re.fullmatch(r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)", declared), declared
    assert actual_name == declared == "1.0.7", (variant, actual_name, declared)
    actual_code = int(re.search(r"versionCode='([0-9]+)'", badging).group(1))
    expected_code = int(os.environ.get("TEST_VERSION_CODE", "1")) if variant == "debugTest" else 1
    assert actual_code == expected_code, (variant, actual_code, expected_code)
    if expected_label:
        assert f"application-label:'{expected_label}'" in badging, badging
    manifests = list(Path("app/build/intermediates/merged_manifests").glob(f"{variant}/**/AndroidManifest.xml"))
    assert len(manifests) == 1, manifests
    root = ET.parse(manifests[0]).getroot()
    assert root.get("package") == expected_id
    assert root.get(ANDROID + "sharedUserId") is None
    app = root.find("application")
    assert app.get(ANDROID + "process") in (None, expected_id)
    providers = app.findall("provider")
    authorities = set()
    for provider in providers:
        assert provider.get(ANDROID + "exported") == "false", provider.attrib
        for authority in provider.get(ANDROID + "authorities", "").split(";"):
            assert authority.startswith(expected_id + "."), authority
            authorities.add(authority)
    if variant == "debugTest":
        assert app.get(ANDROID + "allowBackup") == "false"
        assert app.get(ANDROID + "dataExtractionRules") == "@xml/test_data_extraction_rules"
        assert expected_id + ".testbackups" in authorities
        paths = ET.parse("app/src/debugTest/res/xml/test_backup_paths.xml").getroot()
        assert len(paths) == 1
        assert paths[0].tag == "files-path" and paths[0].get("path") == "test-backups/"
    else:
        assert not any(x.endswith(".testbackups") for x in authorities)
    print(f"Verified {apk}: applicationId={actual_id}, versionName={actual_name}, versionCode={actual_code}, authorities={sorted(authorities)}")
    return authorities


main = verify("debug", "ru.fo6osik.workjournal")
test = verify("debugTest", "ru.fo6osik.workjournal.debugtest", "АгроНализ TEST")
assert not main.intersection(test)
print("APK identifier/provider isolation SUCCESS")
