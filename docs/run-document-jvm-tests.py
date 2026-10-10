"""Optional cached-Kotlin JVM fallback. Prefer Gradle testDebugUnitTest for normal builds.

Requires a Gradle 8.13 distribution with Kotlin compiler jars and previously cached
JUnit, coroutines and annotations. Does not download dependencies.
"""
import os
from pathlib import Path
import subprocess
import tempfile


def first(paths, description):
    result = next(iter(paths), None)
    if result is None:
        raise RuntimeError("Missing cached {}. Run the normal Gradle tests or prepare the cache.".format(description))
    return result


def main():
    root = Path(__file__).resolve().parents[1]
    gradle_home = Path(os.environ.get("GRADLE_USER_HOME", str(Path.home() / ".gradle")))
    android_home = Path(os.environ.get("ANDROID_HOME", os.environ.get("ANDROID_SDK_ROOT", "/opt/android_sdk")))
    cache = gradle_home / "caches/modules-2/files-2.1"
    lib = first((gradle_home / "wrapper/dists").glob("gradle-8.13-bin/*/gradle-8.13/lib"), "Gradle 8.13 distribution")
    coroutines = first(cache.glob("org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.9.0/*/*.jar"), "coroutines 1.9.0")
    annotations = first(cache.glob("org.jetbrains/annotations/13.0/*/*.jar"), "annotations 13.0")
    android = android_home / "platforms/android-36/android.jar"
    required = [lib / "kotlin-stdlib-2.0.21.jar", lib / "junit-4.13.2.jar",
                lib / "hamcrest-core-1.3.jar", android, annotations, coroutines]
    missing = [str(path) for path in required if not path.is_file()]
    if missing:
        raise RuntimeError("Missing required cached files: {}. Use Gradle tests instead.".format(", ".join(missing)))
    classpath = os.pathsep.join(str(path) for path in required)
    sources = list((root / "app/src/main/java/ru/fo6osik/workjournal/documents").rglob("*.kt"))
    sources += list((root / "app/src/test/java/ru/fo6osik/workjournal/documents").rglob("*.kt"))
    sources.append(root / "app/src/test/java/ru/fo6osik/workjournal/ExampleUnitTest.kt")
    classes = [
        "ru.fo6osik.workjournal.documents.GoogleDriveDocumentSourceTest",
        "ru.fo6osik.workjournal.documents.DocumentCatalogTest",
        "ru.fo6osik.workjournal.documents.DocumentRepositoryTest",
        "ru.fo6osik.workjournal.ExampleUnitTest"
    ]
    print("Fallback: Kotlin 2.0.21, JVM 1.8, Android 36 stubs, coroutines 1.9.0, JUnit 4.13.2", flush=True)
    with tempfile.TemporaryDirectory(prefix="agronaliz-document-jvm-") as output:
        subprocess.run([
            "java", "-cp", str(lib / "*"), "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
            "-no-stdlib", "-no-reflect", "-jvm-target", "1.8",
            "-classpath", classpath, "-d", output
        ] + [str(path) for path in sources], check=True)
        subprocess.run([
            "java", "-cp", output + os.pathsep + classpath, "org.junit.runner.JUnitCore"
        ] + classes, check=True)


if __name__ == "__main__":
    main()
