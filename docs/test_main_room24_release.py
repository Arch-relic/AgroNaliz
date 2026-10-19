"""Execute the live publisher's inline preflight/signer with fake APIs/tools, never real secrets."""
import copy
import importlib.util
import json
import os
from pathlib import Path
import shutil
import tempfile
import unittest
from unittest.mock import patch

spec=importlib.util.spec_from_file_location('historical_policy_harness',Path(__file__).with_name('test_main_room23_policy_patch.py'))
h=importlib.util.module_from_spec(spec);spec.loader.exec_module(h)
ROOT=Path(__file__).resolve().parents[1]
STEPS=['Build disposable future MAIN TEST fixtures and instrumentation',
       'Build pinned Room23 fixtures without keys or publication',
       'Verify Room23 to24 both packages retain data and restart']

class MainRoom24Release(unittest.TestCase):
    def setUp(self):
        temp=tempfile.TemporaryDirectory(prefix='main24-policy-');self.addCleanup(temp.cleanup)
        self.stage=Path(temp.name)
        for name in ['app/build.gradle.kts','app/src/main/java/ru/fo6osik/workjournal/AppDatabase.kt']:
            dest=self.stage/name;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(ROOT/name,dest)
        self.workflow=(ROOT/'.github/workflows/main-apk-publish.yml').read_text()
        self.preflight=h.inline(self.workflow)
        self.signer={'__name__':'synthetic_room24_signer'}
        exec(compile(h.inline(self.workflow.split('      - name: Sign and verify stable MAIN certificate\n',1)[1]),'synthetic-signer','exec'),self.signer)
        self.audit_run=dict(id=123,workflow_id=389125371,path='.github/workflows/room24-update-checks.yml',
            head_sha='a'*40,head_branch='main',event='workflow_dispatch',run_attempt=1,status='completed',conclusion='success',
            repository=dict(full_name='Arch-relic/AgroNaliz'),head_repository=dict(full_name='Arch-relic/AgroNaliz'),
            actor=dict(login='Arch-relic'),triggering_actor=dict(login='Arch-relic'))
        self.jobs=dict(total_count=1,jobs=[dict(name='room24-update',status='completed',conclusion='success',
            steps=[dict(name=n,status='completed',conclusion='success') for n in STEPS])])
    def preflight_run(self,variables=None,previous=(5,),room=24,api_error=None):
        env=dict(GITHUB_RUN_NUMBER='6',INSTALLED_CODE='3000005',INSTALLED_ROOM='23')
        env.update(variables or {})
        return h.MainRoom23PolicyPatchTest.preflight_run(self,env,previous,room,api_error)
    def signer_run(self,variables=None,**kwargs):
        env=dict(INSTALLED_CODE='3000005',INSTALLED_ROOM='23',MAIN_ROOM_VERSION='24')
        env.update(variables or {})
        return h.MainRoom23PolicyPatchTest.signer_run(self,variables=env,**dict(dict(version=3000006,name='1.0.9'),**kwargs))
    def test_exact_audit_authorizes_only_23_to24(self):
        self.assertIn('version_code=3000006',self.preflight_run())
        info=self.signer_run()
        for key,value in dict(versionName='1.0.9',roomVersion=24,installedRoomVersion=23,auditRunId=123,
                auditSourceSha='a'*40,auditWorkflowId=389125371,applicationId='ru.fo6osik.workjournal').items():
            self.assertEqual(value,info[key])
    def test_audit_identity_sha_status_actor_attempt_and_fork(self):
        h.MainRoom23PolicyPatchTest.test_audit_identity_status_and_actor_mismatches_rejected(self)
    def test_api_failure_is_fail_closed(self):
        h.MainRoom23PolicyPatchTest.test_api_failures_block_before_build_output(self)
    def test_required_jobs_steps_cannot_be_missing_skipped_failed_or_truncated(self):
        good=copy.deepcopy(self.jobs)
        bads=[dict(total_count=0,jobs=[]),dict(total_count=2,jobs=good['jobs']),dict(total_count=2,jobs=good['jobs']*2)]
        for n in range(len(STEPS)):
            for conclusion in ['failure','skipped',None]:
                bad=copy.deepcopy(good);bad['jobs'][0]['steps'][n]['conclusion']=conclusion;bads.append(bad)
        for bad in bads:
            self.jobs=bad
            with self.subTest(bad=bad),self.assertRaises(SystemExit):self.preflight_run()
    def test_owner_main_first_manual_dispatch_only(self):
        for env in [dict(GITHUB_REF='refs/heads/feature/release'),dict(GITHUB_ACTOR='other'),dict(GITHUB_RUN_ATTEMPT='2'),
                    dict(GITHUB_EVENT_NAME='pull_request'),dict(GITHUB_REPOSITORY='fork/repo'),dict(GITHUB_REPOSITORY_OWNER='other')]:
            with self.subTest(env=env),self.assertRaises(SystemExit):self.preflight_run(env)
    def test_no_audit_downgrade_unsupported_room_low_code_or_stale_run(self):
        for env in [dict(AUDIT_RUN_ID=''),dict(AUDIT_RUN_ID='0'),dict(INSTALLED_ROOM='22'),dict(INSTALLED_ROOM='24'),
                    dict(INSTALLED_ROOM='25'),dict(INSTALLED_CODE='3000004'),dict(INSTALLED_CODE='3000006')]:
            with self.subTest(env=env),self.assertRaises(SystemExit):self.preflight_run(env)
        for room in (21,25,999):
            with self.subTest(room=room),self.assertRaises(SystemExit):self.preflight_run(room=room)
        with self.assertRaises(SystemExit):self.preflight_run(previous=(6,))
        p=self.stage/'app/build.gradle.kts';p.write_text(p.read_text().replace('1.0.9','1.0.8'))
        with self.assertRaises(SystemExit):self.preflight_run()
    def test_wrong_apk_cert_code_package_version_and_source_blocked(self):
        for kwargs in [dict(package='ru.fo6osik.workjournal.debugtest'),dict(signed_package='ru.fo6osik.workjournal.debugtest'),
                       dict(certificate='00'*32),dict(version=3000005),dict(name='1.0.8'),
                       dict(variables=dict(INSTALLED_ROOM='22')),dict(variables=dict(MAIN_ROOM_VERSION='25')),
                       dict(variables=dict(MAIN_MIGRATION_AUDIT_RUN_ID='')),dict(variables=dict(APK_SOURCE_SHA='invalid'))]:
            with self.subTest(kwargs=kwargs),self.assertRaises(self.signer['SigningError']):self.signer_run(**kwargs)
            self.assertFalse((self.stage/'sign-run/agronaliz-main-output').exists())
            self.assertEqual([],list((self.stage/'sign-run').glob('agronaliz-main-key-*')))
    def test_test_package_rejected_before_decoding_any_key(self):
        with patch('base64.b64decode',side_effect=AssertionError('Key must not be accessed')):
            with self.assertRaises(self.signer['SigningError']):self.signer_run(package='ru.fo6osik.workjournal.debugtest')
    def test_original_room22_and_room23_rules_preserved(self):
        for room in (20,21,22):
            self.assertIn('room_version=22',self.preflight_run(dict(INSTALLED_ROOM=str(room),INSTALLED_CODE='53',AUDIT_RUN_ID=''),room=22))
        self.audit_run.update(workflow_id=387641531,path='.github/workflows/main-room23-checks.yml')
        self.jobs['jobs'][0].update(name='main-room23',steps=[dict(name=n,status='completed',conclusion='success') for n in h.STEPS])
        p=self.stage/'app/build.gradle.kts';p.write_text(p.read_text().replace('1.0.9','1.0.8'))
        self.assertIn('room_version=23',self.preflight_run(dict(INSTALLED_ROOM='22',INSTALLED_CODE='3000003'),room=23))
        for env in [dict(INSTALLED_ROOM='21'),dict(INSTALLED_ROOM='23'),dict(INSTALLED_ROOM='22',INSTALLED_CODE='3000002'),dict(INSTALLED_ROOM='22',AUDIT_RUN_ID='')]:
            with self.subTest(legacy=env),self.assertRaises(SystemExit):self.preflight_run(env,room=23)
        self.assertEqual('1.0.8',self.signer_run(name='1.0.8',variables=dict(INSTALLED_ROOM='22',INSTALLED_CODE='3000003',MAIN_ROOM_VERSION='23'))['versionName'])
    def test_secret_boundaries_and_required_audit_steps_remain(self):
        h.MainRoom23PolicyPatchTest.test_original_guards_and_secret_isolation_unchanged(self)
        for step in STEPS:self.assertIn(step,(ROOT/'.github/workflows/room24-update-checks.yml').read_text())

if __name__=='__main__':unittest.main()
