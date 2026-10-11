"""Real v20 -> v21 TEST APK update and harvest tests, only on the disposable CI emulator.

Run AFTER run-build-isolation-checks.py (which uninstalls TEST and leaves synthetic main).
Never install, remove or migrate a user's existing application.
"""
import re
from pathlib import Path
from runpy import run_path

isolation = run_path(str(Path(__file__).with_name("run-build-isolation-checks.py")))
adb = isolation["adb"]


def instrument(test_class, method=None, count=1):
    selector = "ru.fo6osik.workjournal." + test_class + ("#" + method if method else "")
    output = adb("shell", "am", "instrument", "-w", "-e", "class", selector,
                 "ru.fo6osik.workjournal.debugtest.test/androidx.test.runner.AndroidJUnitRunner")
    expected = rf"OK \({count} test{'s' if count != 1 else ''}\)"
    if not re.search(expected, output) or "FAILURES!!!" in output:
        raise RuntimeError("Harvest instrumentation failed: " + selector)


def main():
    test_id = "ru.fo6osik.workjournal.debugtest"
    if adb("shell", "getprop", "ro.kernel.qemu").strip() != "1":
        raise RuntimeError("Harvest checks require a disposable emulator, never a physical device")
    packages = set(adb("shell", "pm", "list", "packages").splitlines())
    if "package:" + test_id in packages:
        raise RuntimeError("Refusing to replace an existing TEST installation")
    legacy = Path(".migration-v20/app/build/outputs/apk/debugTest/app-debugTest.apk")
    assert legacy.is_file(), legacy
    adb("install", str(legacy))
    adb("install", "-r", "app/build/outputs/apk/androidTest/debugTest/app-debugTest-androidTest.apk")
    instrument("HarvestMigrationTest", "seedV20")
    # Same applicationId and CI debug key: Android retains the v20 database and preferences.
    adb("install", "-r", "app/build/outputs/apk/debugTest/app-debugTest.apk")
    instrument("HarvestMigrationTest", "verifyV21AndRestoreLegacyBackup")
    instrument("HarvestPersistenceTest", count=5)
    adb("shell", "pm", "grant", test_id, "android.permission.POST_NOTIFICATIONS")
    adb("shell", "appops", "set", test_id, "SCHEDULE_EXACT_ALARM", "allow")
    instrument("HarvestAreaUiTest", count=5)
    instrument("HarvestBackupTest", count=3)
    isolation["instrument"]("ru.fo6osik.workjournal", "verifyMainAfterTest")
    adb("uninstall", test_id)
    isolation["instrument"]("ru.fo6osik.workjournal", "verifyMainAfterTest")
    print("HARVEST SUCCESS: 15 harvest instrumentation tests; real Room v20 -> v21 upgrade; "
          "all legacy rows/timers/events retained; optional area completion/editing; "
          "v20/v21 backups and v21 import; synthetic main unchanged", flush=True)


if __name__ == "__main__":
    main()
