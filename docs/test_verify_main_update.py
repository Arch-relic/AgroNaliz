import hashlib
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch
import subprocess

spec=importlib.util.spec_from_file_location('verify_main',Path(__file__).with_name('verify-main-apk-update.py'))
v=importlib.util.module_from_spec(spec);spec.loader.exec_module(v)

class VerifyMainUpdate(unittest.TestCase):
    def report(self):
        return dict(applicationId=v.APP,versionCode=3000001,certificateSha256=v.CERT,apkSha256=hashlib.sha256(b'fixture').hexdigest(),
                    roomVersion=22,installedVersionCode=53,installedRoomVersion=20,sourceSha='a'*40)
    def check(self,report=None,old=None,new=None,room=20):
        v.validate(report or self.report(),b'fixture',old or (53,v.CERT),new or (3000001,v.CERT),room)
    def test_valid_53_room20_to_higher_room21(self):self.check()
    def test_room21_same_schema(self):
        r=self.report();r['installedRoomVersion']=21;self.check(r,room=21)
    def test_wrong_old_or_new_certificate(self):
        for old,new in [((53,'A'*64),None),(None,(3000001,'A'*64))]:
            with self.assertRaises(ValueError):self.check(old=old,new=new)
    def test_equal_or_lower_version_rejected(self):
        for code in [52,53]:
            with self.assertRaises(ValueError):self.check(new=(code,v.CERT))
    def test_report_wrong_hash_package_or_version(self):
        for key,val in [('applicationId',v.APP+'.debugtest'),('apkSha256','0'*64),('versionCode',1),('installedVersionCode',1),('installedRoomVersion',21),('sourceSha','missing')]:
            r=self.report();r[key]=val
            with self.assertRaises(ValueError):self.check(r)
    def test_room_downgrade_or_unknown(self):
        for room in [19,23]:
            with self.assertRaises(ValueError):self.check(room=room)
        r=self.report();r['roomVersion']=20
        with self.assertRaises(ValueError):self.check(r)
    def test_tool_error_suppresses_private_details(self):
        with patch('subprocess.run',return_value=subprocess.CompletedProcess([],1,'private','private')):
            with self.assertRaises(ValueError) as e:v.tool(['adb'])
            self.assertNotIn('private',str(e.exception).replace('private output suppressed',''))
    def test_read_only_script_has_no_install_clear_or_live_db(self):
        s=Path(__file__).with_name('verify-main-apk-update.py').read_text()
        for command in ["'install'","'uninstall'","'clear'","'run-as'","'sqlite3'"]:self.assertNotIn(command,s)
