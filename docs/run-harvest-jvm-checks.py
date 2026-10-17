"""Offline fallback for pure harvest calculations when Gradle cannot start.

This runs actual Kotlin/JUnit sources using an existing Gradle distribution's compiler.
It does not compile Android UI/Room code and does not replace Android/Gradle checks.
"""
import argparse
from pathlib import Path
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--gradle-lib', type=Path, required=True)
    args = parser.parse_args()
    lib = args.gradle_lib.resolve()
    for pattern in ('kotlin-compiler-embeddable-*.jar', 'junit-4*.jar', 'kotlin-stdlib-*.jar'):
        if not list(lib.glob(pattern)):
            raise SystemExit('Required local library missing: ' + pattern)
    root = Path(__file__).resolve().parents[1]
    classpath = ':'.join(str(p) for p in sorted(lib.glob('*.jar')))
    source = root / 'app/src/main/java/ru/fo6osik/workjournal'
    tests = root / 'app/src/test/java/ru/fo6osik/workjournal'
    names = ['HarvestCalculator', 'HarvestCampaignMetrics', 'HarvestResultsFormatter',
             'HarvestBackupCompatibility', 'HarvestDensityCatalog']
    test_names = ['HarvestCalculatorTest', 'HarvestCampaignMetricsTest']
    with tempfile.TemporaryDirectory(prefix='harvest-jvm-') as folder:
        command = ['java', '-cp', classpath, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
                   '-no-stdlib', '-no-reflect', '-jvm-target', '17', '-classpath', classpath, '-d', folder]
        subprocess.run(command + [str(source / (name + '.kt')) for name in names] +
                       [str(tests / (name + '.kt')) for name in test_names], check=True)
        subprocess.run(['java', '-cp', folder + ':' + classpath, 'org.junit.runner.JUnitCore'] +
                       ['ru.fo6osik.workjournal.' + name for name in test_names], check=True)


if __name__ == '__main__':
    main()
