import copy
from dataclasses import asdict, replace
import unittest
from agent_core.v2.controller import OfflineController
from agent_core.v2.model import Rejected
from v2_fakes import Harness, move


class V2ControllerTests(unittest.TestCase):
    def denied(self,h,reason=None):
        result=h.run()
        self.assertEqual('denied',result.outcome,result)
        self.assertEqual([],h.transport.operations)
        if reason:self.assertEqual(reason,result.reason)
        self.assertNotIn('CANARY',str(asdict(result)))
        return result

    def test_downscope_and_revoke(self):
        h=Harness();r=h.run()
        self.assertEqual(('allowed','revoked',True),(r.outcome,r.cleanup,r.offline))
        self.assertEqual([9004],h.transport.operations)
        self.assertEqual([dict(installation_id=9002,repository_ids=(9004,),permissions=h.registry['permissions'])],h.credentials.acquisitions)
        self.assertEqual(1,len(h.credentials.revocations))

    def test_default_credentials_disabled(self):
        h=Harness();r=OfflineController(h.transport,h.clock,h.anchors).simulate(h.task,'test',h.execution)
        self.assertEqual('LIVE_CREDENTIALS_DISABLED',r.reason)
        self.assertEqual([],h.transport.operations)

    def test_fixed_operation(self):
        for operation in ['implement','technical_fix','draft_pr','room','backup','signing','production_policy',
                          'merge_main','publish_apk','read_identity_smoke','test; id','https://example.org','../script',None]:
            with self.subTest(operation=operation):
                h=Harness();r=OfflineController(h.transport,h.clock,h.anchors,h.credentials).simulate(h.task,operation,h.execution)
                self.assertEqual('OPERATION_FORBIDDEN',r.reason)
                self.assertEqual([],h.credentials.acquisitions);self.assertEqual([],h.transport.operations)

    def test_snapshot_revoked(self):
        for state in ['blocked','cancelled','awaiting_approval']:
            h=Harness();snapshot=copy.deepcopy(h.task);move(h.task,state)
            self.denied(h,'TASK_NOT_AUTHORIZED')
            h=Harness();snapshot=copy.deepcopy(h.task);move(h.records['task/AGN-9001@'+'b'*40],state)
            r=OfflineController(h.transport,h.clock,h.anchors,h.credentials).simulate(snapshot,'test',h.execution)
            self.assertEqual('STALE_TASK_SNAPSHOT',r.reason)

    def test_new_revision_revokes(self):
        h=Harness();new=copy.deepcopy(h.task['revisions'][0]);new['spec']['id']='r2'
        from agent_core.core import digest
        new['hash']=digest(new['spec']);h.task['revisions'].append(new);h.task['current_revision']='r2'
        move(h.task,'awaiting_approval');self.denied(h)

    def test_main_race(self):
        h=Harness()
        def hook(t,resource):
            if resource=='main' and t.calls.count('main')==2:t.records['main']='e'*40
        h.transport.hook=hook
        self.denied(h,'MAIN_CHANGED');self.assertEqual(1,len(h.credentials.revocations))

    def test_second_action_revalidates(self):
        h=Harness();self.assertEqual('allowed',h.run().outcome)
        move(h.task,'blocked');r=h.run()
        self.assertEqual('denied',r.outcome);self.assertEqual([9004],h.transport.operations)

    def test_fault_at_every_read(self):
        baseline=Harness();self.assertEqual('allowed',baseline.run().outcome)
        for index in range(1,len(baseline.transport.calls)+1):
            for error in [TimeoutError('CANARY'),PermissionError('401 CANARY'),OSError('403 CANARY'),
                          OSError('404'),OSError('429'),OSError('500'),ValueError('bad JSON CANARY')]:
                with self.subTest(index=index,error=type(error).__name__):
                    h=Harness();h.transport.fault_at=index;h.transport.error=error
                    self.denied(h,'TRUST_DATA_UNAVAILABLE')
                    self.assertEqual(len(h.credentials.acquisitions),len(h.credentials.revocations))

    def test_missing_records(self):
        for resource in list(Harness().records):
            with self.subTest(resource=resource):
                h=Harness();del h.records[resource];self.denied(h)

    def test_policy_registry_tamper(self):
        for resource in ['registry@'+'b'*40,'policy@'+'b'*40]:
            h=Harness();h.records[resource]['version']='changed';self.denied(h,'TRUSTED_DIGEST_MISMATCH')

    def test_installation_identity_scope_status(self):
        cases={'app_id':[999,'9001',True], 'installation_id':[999], 'installation_account_id':[999],
               'repository_ids':[[9004,999],[]], 'repository_selection':['all'], 'complete':[False,None],
               'permissions':[{'contents':'write'},{}], 'suspended':[True,None], 'active':[False]}
        for key,values in cases.items():
            for value in values:
                with self.subTest(key=key,value=value):
                    h=Harness();h.records['installation'][key]=value;self.denied(h)

    def test_revocation_during_check(self):
        h=Harness()
        def hook(t,resource):
            if resource=='installation' and t.calls.count(resource)==2:t.records[resource]['suspended']=True
        h.transport.hook=hook;self.denied(h,'INSTALLATION_UNAVAILABLE')
        self.assertEqual(1,len(h.credentials.revocations))

    def test_lease_scope(self):
        for kwargs in [dict(repository_ids=(9004,999)),dict(permissions={'contents':'write'}),dict(installation_id=99)]:
            h=Harness();h.credentials.lease=replace(h.credentials.lease,**kwargs)
            self.denied(h,'LEASE_SCOPE_MISMATCH');self.assertEqual(1,len(h.credentials.revocations))

    def test_lease_expired(self):
        h=Harness();h.credentials.lease=replace(h.credentials.lease,expires_at=h.clock.utc)
        self.denied(h,'LEASE_EXPIRED')

    def test_consent_boundaries(self):
        for now in ['2026-10-21T11:59:59Z','2026-10-21T20:00:00Z','2026-10-21T20:00:01Z']:
            h=Harness();h.clock.utc=h.records['server_time']=now;self.denied(h,'CONSENT_NOT_CURRENT')
        h=Harness();h.clock.utc=h.records['server_time']='2026-10-21T12:00:00Z'
        # Run not yet created: consent boundary alone cannot authorize execution.
        self.denied(h,'CONTROLLER_RUN_EXPIRED')

    def test_clock_skew(self):
        h=Harness();h.records['server_time']='2026-10-21T12:01:21Z';self.denied(h,'CLOCK_SKEW')
        h=Harness();h.records['server_time']='2026-10-21T12:01:20Z';self.assertEqual('allowed',h.run().outcome)

    def test_session_deadline(self):
        for elapsed in [600,-1]:
            h=Harness()
            def hook(t,resource):h.clock.elapsed=elapsed
            h.transport.hook=hook;self.denied(h,'SESSION_EXPIRED')

    def test_consent_expires_during_check(self):
        h=Harness()
        def hook(t,resource):
            if resource=='main' and t.calls.count(resource)==2:
                h.clock.utc=t.records['server_time']='2026-10-21T20:00:00Z'
        h.transport.hook=hook;self.denied(h,'CONSENT_NOT_CURRENT')

    def test_fake_execution_environment(self):
        for key,value in [('run_id',9010),('repository','fork/repo'),('ref','refs/heads/feature'),
                          ('sha','e'*40),('workflow_ref','forged'),('event','pull_request'),
                          ('attempt',2),('actor_id',9001)]:
            with self.subTest(key=key):
                h=Harness();h.execution[key]=value;self.denied(h)

    def test_worker_credentials_not_returned(self):
        h=Harness();result=asdict(h.run())
        self.assertEqual({'outcome','reason','at','main_sha','task_id','context_hash','token_expires_at','cleanup','offline'},set(result))
        self.assertNotIn('handle',repr(h.credentials.lease))

    def test_exception_cleanup(self):
        h=Harness();h.transport.smoke_error=OSError('CANARY')
        r=h.run();self.assertEqual('denied',r.outcome);self.assertEqual('revoked',r.cleanup)
        self.assertNotIn('CANARY',str(r))

    def test_cancellation_cleanup(self):
        h=Harness();h.transport.smoke_error=KeyboardInterrupt()
        with self.assertRaises(KeyboardInterrupt):h.run()
        self.assertEqual(1,len(h.credentials.revocations))

    def test_revoke_failure_blocks(self):
        h=Harness();h.credentials.revoke_error=OSError('CANARY')
        result=h.run();self.assertEqual(('blocked','CLEANUP_FAILED','failed'),(result.outcome,result.reason,result.cleanup))
        self.assertEqual(h.credentials.lease.expires_at,result.token_expires_at)
        self.assertNotIn('CANARY',str(result))

    def test_safe_audit_on_malformed_values(self):
        for resource in ['main','server_time']:
            h=Harness();h.records[resource]='CANARY'
            self.denied(h)
        h=Harness();h.credentials.lease=replace(h.credentials.lease,expires_at='CANARY')
        self.denied(h,'INVALID_TIME')
        h=Harness();h.transport.fault_at=1;h.transport.error=Rejected('CANARY')
        self.denied(h,'TRUST_DATA_UNAVAILABLE')

    def test_boolean_execution_fields(self):
        for field in ['run_id','attempt','actor_id']:
            h=Harness();h.execution[field]=True;self.denied(h,'EXECUTION_CONTEXT_MISMATCH')

    def test_controller_metadata_tamper(self):
        for field,value in [('actor',{'id':9001,'type':'Bot'}),('source_sha','e'*40),
                            ('workflow_id',9005),('status','completed'),('event','pull_request'),
                            ('head_branch','feature'),('run_attempt',2),
                            ('created_at','2026-10-21T11:00:00Z')]:
            with self.subTest(field=field):
                h=Harness();h.records['run/9011'][field]=value;self.denied(h)

    def test_incomplete_installation_at_each_read(self):
        for key in list(Harness().records['installation']):
            for occurrence in [1,2]:
                with self.subTest(key=key,occurrence=occurrence):
                    h=Harness()
                    def hook(t,resource):
                        if resource=='installation' and t.calls.count(resource)==occurrence:
                            del t.records[resource][key]
                    h.transport.hook=hook;self.denied(h)

    def test_acquire_failure_no_operation(self):
        h=Harness();h.credentials.acquire_error=OSError('CANARY')
        self.denied(h,'TRUST_DATA_UNAVAILABLE')

    def test_token_does_not_extend_consent(self):
        h=Harness();h.credentials.lease=replace(h.credentials.lease,expires_at='2026-10-22T12:00:00Z')
        h.clock.utc=h.records['server_time']='2026-10-21T20:00:00Z'
        self.denied(h,'CONSENT_NOT_CURRENT')

    def test_aba_is_not_claimed_detectable(self):
        h=Harness()
        def hook(t,resource):
            if resource=='installation':
                # A change and restoration between observations is invisible.
                t.records['main']='e'*40
                t.records['main']='b'*40
        h.transport.hook=hook
        self.assertEqual('allowed',h.run().outcome)
        self.assertEqual([9004],h.transport.operations)

    def test_lease_expires_during_verification(self):
        h=Harness()
        h.credentials.lease=replace(h.credentials.lease,expires_at='2026-10-21T12:00:21Z')
        def hook(t,resource):
            if resource=='main' and t.calls.count(resource)==2:
                h.clock.utc=t.records['server_time']='2026-10-21T12:00:21Z'
        h.transport.hook=hook;self.denied(h,'LEASE_EXPIRED')
