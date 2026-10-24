import copy
import unittest
from agent_core.core import digest
from agent_core.v2.model import Rejected
from agent_core.v2.task import validate_task
from v2_fakes import Harness, move


class V2ApprovalTests(unittest.TestCase):
    def denied(self,h):
        result=h.run()
        self.assertEqual('denied',result.outcome, result)
        self.assertEqual([],h.transport.operations)
        return result

    def test_exact_approval(self):
        h=Harness();self.assertEqual('allowed',h.run().outcome)

    def test_approver_namespace(self):
        for actor in [dict(id=9001,type='Bot'),dict(id=240867647,type='Bot'),dict(id=123,type='User'),
                      dict(id=True,type='User'),dict(id='240867647',type='User')]:
            for field in ['actor','triggering_actor']:
                with self.subTest(actor=actor,field=field):
                    h=Harness();h.records['run/9010'][field]=actor;self.denied(h)

    def test_event_workflow_and_title(self):
        cases={'event':['pull_request','schedule','workflow_run'], 'head_branch':['feature'],
               'run_attempt':[2,'1',True], 'status':['in_progress'], 'conclusion':['failure',None],
               'path':['.github/workflows/agent-core.yml'], 'workflow_id':[111],
               'display_title':['approved','approve:AGN-9001'], 'source_sha':['e'*40],
               'source_hash':['e'*64], 'head_sha':['main','../ref']}
        for key,values in cases.items():
            for value in values:
                with self.subTest(key=key,value=value):
                    h=Harness();h.records['run/9010'][key]=value;self.denied(h)

    def test_repository_and_head_repository(self):
        for field in ['repository','head_repository']:
            for key,value in [('id',999),('full_name','foreign/repo')]:
                with self.subTest(field=field,key=key):
                    h=Harness();h.records['run/9010'][field][key]=value;self.denied(h)

    def test_deleted_or_incomplete_evidence(self):
        for key in list(Harness().records['run/9010']):
            with self.subTest(key=key):
                h=Harness();del h.records['run/9010'][key];self.denied(h)
        h=Harness();del h.records['run/9010'];self.denied(h)

    def test_no_implicit_approval(self):
        h=Harness();h.task['history'][-1]['approval']=None
        self.denied(h)

    def test_changed_spec_even_with_new_hash(self):
        h=Harness();revision=h.task['revisions'][0]
        revision['spec']['title']='Changed';revision['hash']=digest(revision['spec'])
        self.denied(h)

    def test_rehashed_context_cannot_change_immutable_request(self):
        h=Harness();q=h.task['approval_requests'][0];q['context']['nonce']='e'*32
        q['context_hash']=digest(q['context']);h.task['history'][-1]['approval'].update(nonce='e'*32,context_hash=q['context_hash'])
        h.records['run/9010']['display_title']='approve-v2:AGN-9001:'+q['context_hash']
        self.assertEqual('IMMUTABLE_REQUEST_MISMATCH',self.denied(h).reason)

    def test_immutable_history_prefix(self):
        h=Harness();h.task['history'][0]['reason']='Rewritten';self.denied(h)

    def test_wrong_history_position(self):
        h=Harness();q=h.task['approval_requests'][0];q['context']['history_position']=3
        q['context_hash']=digest(q['context']);self.denied(h)

    def test_replay_requires_new_run(self):
        h=Harness();move(h.task,'awaiting_approval')
        q=copy.deepcopy(h.task['approval_requests'][0]);q['context'].update(history_position=4,nonce='e'*32)
        q['context_hash']=digest(q['context']);h.task['approval_requests'].append(q)
        move(h.task,'approved',dict(run_id=9010,nonce='e'*32,context_hash=q['context_hash']))
        self.denied(h)

    def test_reused_nonce_new_position(self):
        h=Harness();move(h.task,'awaiting_approval')
        q=copy.deepcopy(h.task['approval_requests'][0]);q['context']['history_position']=4
        q['context_hash']=digest(q['context']);h.task['approval_requests'].append(q)
        self.denied(h)

    def test_approval_run_time_binding(self):
        for key,value in [('created_at','2026-10-21T11:59:59Z'),('updated_at','2026-10-21T20:00:00Z'),
                          ('updated_at','2026-10-21T12:00:11Z')]:
            with self.subTest(key=key,value=value):
                h=Harness();h.records['run/9010'][key]=value;self.denied(h)

    def test_valid_history_states(self):
        for state in ['implementing','review']:
            h=Harness();move(h.task,'implementing')
            if state=='review':move(h.task,'review')
            self.assertEqual('allowed',h.run().outcome)

    def test_new_request_in_approved_denied(self):
        h=Harness();previous=copy.deepcopy(h.task)
        q=copy.deepcopy(h.task['approval_requests'][0]);q['context']['nonce']='e'*32
        q['context_hash']=digest(q['context']);h.task['approval_requests'].append(q)
        with self.assertRaisesRegex(Rejected,'REQUEST_OUTSIDE_AWAITING'):
            validate_task(h.task,previous)

    def test_protected_scope_even_with_consistent_hashes(self):
        for operation in ['implement','draft_pr','room','backup','signing','production_policy','merge_main','publish_apk']:
            with self.subTest(operation=operation):
                h=Harness()
                for task in [h.task,h.original]:
                    revision=task['revisions'][0];revision['spec']['operations']=[operation]
                    revision['hash']=digest(revision['spec'])
                    q=task['approval_requests'][0];q['context']['spec_hash']=revision['hash']
                    q['context']['operations_hash']=digest([operation]);q['context_hash']=digest(q['context'])
                q=h.task['approval_requests'][0]
                h.task['history'][-1]['approval']['context_hash']=q['context_hash']
                h.records['run/9010']['display_title']='approve-v2:AGN-9001:'+q['context_hash']
                self.assertEqual('OPERATION_FORBIDDEN',self.denied(h).reason)
