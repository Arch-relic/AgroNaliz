import importlib.util
import json
from pathlib import Path
import stat
import subprocess
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('test_export', Path(__file__).with_name('export-installed-test-backup.py'))
export = importlib.util.module_from_spec(spec); spec.loader.exec_module(export)

class TestBackupExport(unittest.TestCase):
    def fixture(self, version=20):
        return json.dumps(dict(backupFormatVersion=1,databaseVersion=version,tables={
            'fields':[{'id':1}], 'vehicles':[], 'harvest_processes':[{'id':2,'fieldId':1,'combineVehicleId':None}],
            'harvest_days':[{'id':3,'processId':2}], 'harvest_bunkers':[{'id':4,'dayId':3}]})).encode()
    def test_v20_and_v21_compatible(self):
        for v in [20,21]:self.assertEqual(v,export.validate_backup(self.fixture(v),21)[0])
    def test_room22_and_room23_supported_with_null_historical_areas(self):
        for version in (22, 23):
            data=json.loads(self.fixture(version))
            data['tables'].update(harvest_fuel_days=[],harvest_refuelings=[])
            if version==23:
                data['tables']['harvest_processes'][0].update(fieldAreaSnapshotHa=None,actualPlantedAreaHa=95.0)
            self.assertEqual(version,export.validate_backup(json.dumps(data).encode(),23)[0])
            if version==23:
                with self.assertRaises(ValueError):export.validate_backup(json.dumps(data).encode(),22)

    def test_room23_missing_or_invalid_campaign_area_is_rejected(self):
        for value in ('missing',-1,0,'95',True):
            data=json.loads(self.fixture(23))
            data['tables'].update(harvest_fuel_days=[],harvest_refuelings=[])
            row=data['tables']['harvest_processes'][0]
            row.update(fieldAreaSnapshotHa=None,actualPlantedAreaHa=value)
            if value=='missing':del row['actualPlantedAreaHa']
            with self.assertRaises(ValueError):export.validate_backup(json.dumps(data).encode(),23)

    def test_v21_downgrade_is_blocked(self):
        with self.assertRaises(ValueError):export.validate_backup(self.fixture(21),20)
    def test_broken_relationship_rejected(self):
        data=json.loads(self.fixture());data['tables']['harvest_days'][0]['processId']=999
        with self.assertRaises(ValueError):export.validate_backup(json.dumps(data).encode(),20)
    def test_missing_tables_rejected(self):
        data=json.loads(self.fixture());del data['tables']['vehicles']
        with self.assertRaises(ValueError):export.validate_backup(json.dumps(data).encode(),20)
    def test_duplicate_ids_rejected(self):
        data=json.loads(self.fixture());data['tables']['fields'] *= 2
        with self.assertRaises(ValueError):export.validate_backup(json.dumps(data).encode(),20)
    def test_non_finite_and_bad_format_rejected(self):
        for raw in [b'{"v":NaN}',b'{}',b'not json']:
            with self.assertRaises(ValueError):export.validate_backup(raw,20)
    def test_path_and_shell_injection_rejected_before_adb(self):
        with patch('subprocess.run') as run:
            for name in ['../db.json','x;id.json','/private.json','Agronaliz_TEST_backup_x.json;id']:
                with self.assertRaises(ValueError):export.read_backup('adb','fixture',name)
            run.assert_not_called()
    def test_adb_is_only_read_only_and_failure_suppressed(self):
        with patch('subprocess.run',return_value=subprocess.CompletedProcess([],0,self.fixture(),b'')) as run:
            export.read_backup('adb','fixture','Agronaliz_TEST_backup_abc.json')
            self.assertEqual(['adb','-s','fixture','exec-out','run-as',export.APP_ID,'cat','files/test-backups/Agronaliz_TEST_backup_abc.json'],run.call_args.args[0])
        with patch('subprocess.run',return_value=subprocess.CompletedProcess([],1,b'',b'private device detail')):
            with self.assertRaises(ValueError) as e:export.read_backup('adb','fixture','Agronaliz_TEST_backup_abc.json')
            self.assertNotIn('private device detail',str(e.exception))
    def test_copy_is_private_exact_and_never_overwritten(self):
        # The managed local sandbox mounts /tmp as a Git tree; test write semantics
        # independently from the production destination guard covered below.
        with tempfile.TemporaryDirectory(dir="/tmp") as folder, patch.object(export,"inside_git_root",return_value=False):
            file=Path(folder)/'copy.json';export.save_copy(self.fixture(),file)
            self.assertEqual(self.fixture(),file.read_bytes());self.assertEqual(0o600,stat.S_IMODE(file.stat().st_mode))
            with self.assertRaises(FileExistsError):export.save_copy(b'changed',file)
            self.assertEqual(self.fixture(),file.read_bytes())
    def test_git_destination_refused(self):
        with tempfile.TemporaryDirectory(dir="/tmp") as folder:
            (Path(folder)/'.git').mkdir()
            with self.assertRaises(ValueError):export.save_copy(self.fixture(),Path(folder)/'backup.json')
