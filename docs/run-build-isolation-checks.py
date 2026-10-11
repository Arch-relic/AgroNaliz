"""Install both variants and exercise only synthetic data on a disposable emulator."""
import re
import subprocess
from pathlib import Path


def adb(*args):
    result = subprocess.run(["adb", *args], check=True, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    print(result.stdout, flush=True)
    return result.stdout


def instrument(app_id, method):
    output = adb("shell", "am", "instrument", "-w", "-e", "class",
                 "ru.fo6osik.workjournal.BuildVariantIsolationTest#" + method,
                 app_id + ".test/androidx.test.runner.AndroidJUnitRunner")
    if not re.search(r"OK \(1 test\)", output) or "FAILURES!!!" in output:
        raise RuntimeError("Isolation instrumentation failed: " + app_id + ":" + method)


def main():
    main_id = "ru.fo6osik.workjournal"
    test_id = main_id + ".debugtest"
    if adb("shell", "getprop", "ro.kernel.qemu").strip() != "1":
        raise RuntimeError("Isolation checks require a disposable emulator, never a physical device")
    installed = set(adb("shell", "pm", "list", "packages").splitlines())
    if any("package:" + name in installed for name in [main_id, test_id, main_id + ".test", test_id + ".test"]):
        raise RuntimeError("Refusing to replace existing main/TEST packages; use a fresh emulator")
    for path in [
        "app/build/outputs/apk/debug/app-debug.apk",
        "app/build/outputs/apk/debugTest/app-debugTest.apk",
        "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk",
        "app/build/outputs/apk/androidTest/debugTest/app-debugTest-androidTest.apk",
    ]:
        assert Path(path).is_file(), path
        adb("install", path)
    packages = adb("shell", "pm", "list", "packages")
    for app_id in [main_id, test_id]:
        assert "package:" + app_id in packages.splitlines()
    instrument(main_id, "seedAndCheckSandbox")
    instrument(test_id, "seedAndCheckSandbox")
    instrument(test_id, "testBackupProviderAndExternalWriteGuard")
    instrument(main_id, "verifyMainAfterTest")
    adb("uninstall", test_id)
    instrument(main_id, "verifyMainAfterTest")
    print("ISOLATION SUCCESS: distinct UIDs, Room rows, preferences, files, cache, document store, app-scoped external paths; private TEST backups; main unchanged after TEST uninstall", flush=True)


if __name__ == "__main__":
    main()
