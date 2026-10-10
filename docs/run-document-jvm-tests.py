"""Cloud fallback with cached Kotlin 2.0.21; does not replace the Android build."""
from pathlib import Path
import subprocess
import tempfile


def main():
    root = Path(__file__).resolve().parents[1]
    cache = Path('/workspace/.gradle/caches/modules-2/files-2.1')
    lib = next(Path('/workspace/.gradle/wrapper/dists').glob('gradle-8.13-bin/*/gradle-8.13/lib'))
    coroutines = next(cache.glob('org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.9.0/*/*.jar'))
    annotations = next(cache.glob('org.jetbrains/annotations/13.0/*/*.jar'))
    android = Path('/workspace/android-sdk/platforms/android-36/android.jar')
    classpath = ':'.join(str(path) for path in (
        lib / 'kotlin-stdlib-2.0.21.jar', coroutines,
        lib / 'junit-4.13.2.jar', lib / 'hamcrest-core-1.3.jar', android, annotations
    ))
    sources = list((root / 'app/src/main/java/ru/fo6osik/workjournal/documents').rglob('*.kt'))
    sources += list((root / 'app/src/test/java/ru/fo6osik/workjournal/documents').rglob('*.kt'))
    sources.append(root / 'app/src/test/java/ru/fo6osik/workjournal/ExampleUnitTest.kt')
    classes = [
        'ru.fo6osik.workjournal.documents.GoogleDriveDocumentSourceTest',
        'ru.fo6osik.workjournal.documents.DocumentCatalogTest',
        'ru.fo6osik.workjournal.documents.DocumentRepositoryTest',
        'ru.fo6osik.workjournal.ExampleUnitTest'
    ]
    print('Fallback: Kotlin 2.0.21, JVM 1.8, Android 36 stubs, coroutines 1.9.0, JUnit 4.13.2', flush=True)
    with tempfile.TemporaryDirectory(prefix='agronaliz-document-jvm-') as output:
        subprocess.run([
            'java', '-cp', str(lib / '*'), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
            '-no-stdlib', '-no-reflect', '-jvm-target', '1.8',
            '-classpath', classpath, '-d', output
        ] + [str(path) for path in sources], check=True)
        subprocess.run([
            'java', '-cp', output + ':' + classpath, 'org.junit.runner.JUnitCore'
        ] + classes, check=True)


if __name__ == '__main__':
    main()
