"""Read an existing private TEST JSON backup using the owner's normal adb run-as access.

Never writes to the phone, creates a backup, pulls a live SQLite database, or removes anything.
The destination must be outside Git working trees and must not already exist.
"""
import argparse
import hashlib
import json
import math
import os
from pathlib import Path
import re
import subprocess
import tempfile

APP_ID = 'ru.fo6osik.workjournal.debugtest'
NAME = re.compile(r'Agronaliz_TEST_backup_[A-Za-z0-9_-]+\.json')
REQUIRED = {'fields', 'vehicles', 'harvest_processes', 'harvest_days', 'harvest_bunkers'}


def validate_backup(raw, target_room_version):
    def invalid_constant(_):
        raise ValueError('Non-finite number in backup')
    root = json.loads(raw.decode('utf-8'), parse_constant=invalid_constant)
    if root.get('backupFormatVersion') != 1:
        raise ValueError('Unsupported backup format')
    version = root.get('databaseVersion')
    if type(version) is not int or version not in (20, 21, 22, 23, 24):
        raise ValueError('Only documented Room v20/v21/v22/v23/v24 backups are supported')
    if target_room_version not in (20, 21, 22, 23, 24) or version > target_room_version:
        raise ValueError('Database downgrade blocked; keep the installed application and backup')
    tables = root.get('tables')
    if not isinstance(tables, dict) or not REQUIRED.issubset(tables):
        raise ValueError('Required backup tables are missing')
    if version>=22 and not {'harvest_fuel_days','harvest_refuelings'}.issubset(tables):
        raise ValueError('Fuel tables missing from Room22+ backup')
    for rows in tables.values():
        if not isinstance(rows, list) or any(not isinstance(row, dict) for row in rows):
            raise ValueError('Malformed backup rows')
    def ids(table):
        values = [r.get('id') for r in tables[table]]
        if any(type(v) is not int or v <= 0 for v in values) or len(set(values)) != len(values):
            raise ValueError('Invalid or duplicate row IDs')
        return set(values)
    fields, vehicles, processes, days = [ids(t) for t in ['fields', 'vehicles', 'harvest_processes', 'harvest_days']]
    ids('harvest_bunkers')
    for row in tables['harvest_processes']:
        if row.get('fieldId') is not None and row['fieldId'] not in fields:
            raise ValueError('Broken field relationship')
        if row.get('combineVehicleId') is not None and row['combineVehicleId'] not in vehicles:
            raise ValueError('Broken vehicle relationship')
    if version >= 23:
        for row in tables['harvest_processes']:
            for column in ('fieldAreaSnapshotHa', 'actualPlantedAreaHa'):
                if column not in row:
                    raise ValueError('Room23 campaign area column missing')
                value = row[column]
                if value is not None and (type(value) not in (float, int) or not math.isfinite(value) or value <= 0):
                    raise ValueError('Invalid campaign area')
    if version >= 24:
        for row in tables['harvest_days']:
            step = row.get('reminderStepMinutes')
            if type(step) is not int or step not in (1, 5):
                raise ValueError('Invalid or missing Room24 reminder step')
    if any(r.get('processId') not in processes for r in tables['harvest_days']):
        raise ValueError('Broken day relationship')
    if any(r.get('dayId') not in days for r in tables['harvest_bunkers']):
        raise ValueError('Broken bunker relationship')
    return version, {table: len(rows) for table, rows in tables.items()}


def read_backup(adb, serial, filename):
    if not NAME.fullmatch(filename):
        raise ValueError('Choose an existing TEST backup basename, not a path')
    # Both remote path and package are fixed/validated; never interpolate arbitrary shell text.
    result = subprocess.run([adb, '-s', serial, 'exec-out', 'run-as', APP_ID, 'cat',
                             'files/test-backups/' + filename], stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, timeout=60)
    if result.returncode:
        raise ValueError('Cannot read backup through normal run-as; keep the installed app. No bypass attempted.')
    return result.stdout


def inside_git_root(destination):
    return any((parent / ".git").exists() for parent in Path(destination).resolve().parents)


def save_copy(raw, destination):
    destination = Path(destination).resolve()
    if inside_git_root(destination):
        raise ValueError('Backup copies must stay outside Git working trees')
    # Exclusive final link: never overwrite an existing backup or follow a destination symlink.
    with tempfile.TemporaryDirectory(prefix='.test-backup-', dir=destination.parent) as folder:
        temporary = Path(folder) / 'copy.json'
        fd = os.open(temporary, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, 'wb') as output:
            output.write(raw); output.flush(); os.fsync(output.fileno())
        os.link(temporary, destination)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--serial', required=True, help='Explicit owner-approved device serial')
    parser.add_argument('--backup-name', required=True, help='Existing basename from files/test-backups')
    parser.add_argument('--output', required=True, help='New private JSON file outside Git')
    parser.add_argument('--target-room-version', required=True, type=int, choices=[20, 21, 22, 23, 24])
    args = parser.parse_args()
    raw = read_backup(args.adb, args.serial, args.backup_name)
    version, counts = validate_backup(raw, args.target_room_version)
    save_copy(raw, args.output)
    print('Read-only TEST backup copy saved; Room version:', version,
          'rows:', sum(counts.values()), 'SHA-256:', hashlib.sha256(raw).hexdigest())


if __name__ == '__main__':
    try: main()
    except Exception:
        raise SystemExit('Backup export/check failed; existing application and data were not changed. Check normal adb access and schema compatibility.')
