"""Live same-schema admission: fake APIs and SDK tools, no production signing."""
import importlib.util
from pathlib import Path
import unittest

spec=importlib.util.spec_from_file_location('old_room24',Path(__file__).with_name('test_main_room24_release.py'))
h=importlib.util.module_from_spec(spec);spec.loader.exec_module(h)
STEPS=['Build candidate MAIN1.0.10 TEST fixtures and instrumentation',
       'Build pinned published MAIN1.0.9 Room24 fixtures',
       'Verify Room24 to24 schema data restore and restart']
AUDIT_WORKFLOW_ID=0  # Replaced with GitHub's registered ID before completion.

class MainRoom24SameRelease(unittest.TestCase):
    def setUp(self):
        h.MainRoom24Release.setUp(self)
        p=self.stage/'app/build.gradle.kts';p.write_text(p.read_text().replace('1.0.9','1.0.10'))
        self.audit_run.update(workflow_id=AUDIT_WORKFLOW_ID,path='.github/workflows/main-room24-same-checks.yml')
        self.jobs['jobs'][0].update(name='room24-same-update',steps=[dict(name=n,status='completed',conclusion='success') for n in STEPS])
    def preflight_run(self,variables=None,previous=(7,),room=24,api_error=None):
        env=dict(GITHUB_RUN_NUMBER='8',INSTALLED_CODE='3000007',INSTALLED_ROOM='24');env.update(variables or {})
        return h.MainRoom24Release.preflight_run(self,env,previous,room,api_error)
    def signer_run(self,variables=None,**kwargs):
        env=dict(INSTALLED_CODE='3000007',INSTALLED_ROOM='24',MAIN_ROOM_VERSION='24');env.update(variables or {})
        return h.MainRoom24Release.signer_run(self,env,**dict(dict(version=3000008,name='1.0.10'),**kwargs))
    def test_only_exact_same_schema_audit_allows_1010(self):
        self.assertIn('version_code=3000008',self.preflight_run())
        info=self.signer_run()
        for k,v in dict(versionName='1.0.10',versionCode=3000008,installedRoomVersion=24,roomVersion=24,
                        auditWorkflowId=AUDIT_WORKFLOW_ID,auditRunId=123,auditSourceSha='a'*40).items():
            self.assertEqual(v,info[k])
    test_audit_identity_sha_status_actor_attempt_and_fork=h.MainRoom24Release.test_audit_identity_sha_status_actor_attempt_and_fork
    test_api_failure_is_fail_closed=h.MainRoom24Release.test_api_failure_is_fail_closed
    test_owner_main_first_manual_dispatch_only=h.MainRoom24Release.test_owner_main_first_manual_dispatch_only
    test_test_package_rejected_before_decoding_any_key=h.MainRoom24Release.test_test_package_rejected_before_decoding_any_key
    def test_steps_missing_failed_skipped_and_ambiguous_jobs(self):
        # The historical helper uses its own STEPS only for the number of required steps.
        h.MainRoom24Release.test_required_jobs_steps_cannot_be_missing_skipped_failed_or_truncated(self)
    def test_wrong_floor_schema_audit_and_order_fail_closed(self):
        for env in [dict(INSTALLED_ROOM='23'),dict(INSTALLED_ROOM='22'),dict(INSTALLED_ROOM='25'),
                    dict(INSTALLED_CODE='3000006'),dict(INSTALLED_CODE='3000008'),dict(AUDIT_RUN_ID=''),
                    dict(AUDIT_RUN_ID='0'),dict(AUDIT_RUN_ID='bad')]:
            with self.subTest(env=env),self.assertRaises(SystemExit):self.preflight_run(env)
        for previous in [(8,),(9,)]:
            with self.assertRaises(SystemExit):self.preflight_run(previous=previous)
        for room in (21,25):
            with self.assertRaises(SystemExit):self.preflight_run(room=room)
        self.audit_run.update(workflow_id=389125371,path='.github/workflows/room24-update-checks.yml')
        with self.assertRaises(SystemExit):self.preflight_run()
    def test_other_products_not_admitted(self):
        p=self.stage/'app/build.gradle.kts';source=p.read_text()
        for product in ('1.0.9','1.0.11'):
            p.write_text(source.replace('1.0.10',product))
            with self.subTest(product=product),self.assertRaises(SystemExit):self.preflight_run()
    def test_signer_rejects_bad_baseline_product_cert_package_and_source(self):
        for args in [dict(package='ru.fo6osik.workjournal.debugtest'),dict(signed_package='ru.fo6osik.workjournal.debugtest'),
                     dict(certificate='00'*32),dict(version=3000007),dict(name='1.0.9'),dict(name='1.0.11'),
                     dict(variables=dict(INSTALLED_ROOM='23')),dict(variables=dict(INSTALLED_CODE='3000006')),
                     dict(variables=dict(MAIN_MIGRATION_AUDIT_RUN_ID='')),dict(variables=dict(APK_SOURCE_SHA='wrong'))]:
            with self.subTest(args=args),self.assertRaises(self.signer['SigningError']):self.signer_run(**args)
            self.assertFalse((self.stage/'sign-run/agronaliz-main-output').exists())
            self.assertEqual([],list((self.stage/'sign-run').glob('agronaliz-main-key-*')))
    def test_secret_isolation_and_pinned_baseline_steps(self):
        h.MainRoom24Release.test_secret_boundaries_and_required_audit_steps_remain(self)
        workflow=(h.ROOT/'.github/workflows/main-room24-same-checks.yml').read_text()
        for step in STEPS:self.assertIn(step,workflow)
        self.assertIn('6da84bf09cf0cd3e9d1b5a40887a134bf25d7fe4',workflow)
        self.assertNotIn('secrets.',workflow)

if __name__=='__main__':unittest.main()
