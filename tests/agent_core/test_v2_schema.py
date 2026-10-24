import copy
import unittest
from agent_core.core import digest
from agent_core.v2.model import Rejected, app_task, document, instant, validate_schema, version
from agent_core.v2.task import validate_task
from v2_fakes import fixture, move


class V2SchemaTests(unittest.TestCase):
    def test_three_schemas(self):
        for name, schema in [('request-task', 'task-v2.json'), ('registry', 'registry-v2.json'),
                             ('policy', 'policy-v2.json')]:
            with self.subTest(name=name):
                validate_schema(fixture(name), schema)

    def test_registration_and_request(self):
        task = fixture('request-task')
        self.assertIsNone(validate_task(task))
        task['history'] = task['history'][:1]
        task['state'], task['approval_requests'] = 'proposed', []
        self.assertIsNone(validate_task(task))

    def test_strict_json(self):
        for raw in ['{"x":1,"x":2}', '{"x":NaN}', '{"x":Infinity}', '{"x":1.0}', '{broken']:
            with self.subTest(raw=raw), self.assertRaises(Rejected):
                document(raw)

    def test_versions(self):
        for value in [{}, {'schema_version':True}, {'schema_version':2.0}, {'schema_version':3}]:
            with self.subTest(value=value), self.assertRaises(Rejected):
                version(value)

    def test_identity_types(self):
        for value in [True, '9001', 9001.0, 0, -1, None]:
            task = fixture('request-task')
            task['executor']['app_id'] = value
            with self.subTest(value=value), self.assertRaises(Rejected):
                app_task(task)

    def test_extra_properties(self):
        for key in ['executor_id', 'trusted', 'token', 'command']:
            task = fixture('request-task'); task[key] = 'forged'
            with self.subTest(key=key), self.assertRaises(Rejected):
                app_task(task)

    def test_fixed_policy(self):
        for key, value in [('handler','shell'), ('operations',['implement']), ('session_seconds',601),
                           ('consent_seconds',86400), ('clock_skew_seconds',61)]:
            policy = fixture('policy'); policy[key] = value
            with self.subTest(key=key), self.assertRaises(Rejected):
                validate_schema(policy, 'policy-v2.json')

    def test_strict_utc(self):
        for value in ['2026-10-21T12:00:00+00:00', '2026-10-21T12:00:00.1Z',
                      '2026-02-30T00:00:00Z', '2026-1-1T00:00:00Z']:
            with self.subTest(value=value), self.assertRaises(Rejected): instant(value)

    def test_revision_hash(self):
        task = fixture('request-task'); task['revisions'][0]['spec']['title'] = 'Changed'
        with self.assertRaisesRegex(Rejected, 'SPEC_HASH_MISMATCH'): validate_task(task)

    def test_duplicate_revision(self):
        task = fixture('request-task'); task['revisions'] *= 2
        with self.assertRaisesRegex(Rejected, 'DUPLICATE_REVISION'): validate_task(task)

    def test_append_only(self):
        previous = fixture('request-task')
        for field in ['revisions', 'history', 'approval_requests']:
            task = copy.deepcopy(previous); task[field] = []
            with self.subTest(field=field), self.assertRaises(Rejected): validate_task(task, previous)

    def test_immutable_subject_and_identity(self):
        previous = fixture('request-task')
        for key in ['app_id','installation_id','installation_account_id','repository_id','policy_version']:
            task = copy.deepcopy(previous)
            task['executor'][key] = 'new' if key == 'policy_version' else 9999
            with self.subTest(key=key), self.assertRaisesRegex(Rejected, 'IMMUTABLE_IDENTITY_CHANGED'):
                validate_task(task, previous)

    def test_decision_history(self):
        task = fixture('request-task')
        task['decisions'] = [dict(at='2026-10-21T12:00:00Z', revision='r1', options=['a','b'],
                                  selected='a', reason='Synthetic choice')]
        previous = copy.deepcopy(task)
        validate_task(task)
        task['decisions'][0]['selected'] = 'b'
        with self.assertRaisesRegex(Rejected, 'APPEND_ONLY'): validate_task(task, previous)

    def test_cancel_block_and_terminal(self):
        for state in ['blocked','cancelled']:
            task = fixture('request-task'); move(task,state)
            self.assertIsNone(validate_task(task))
        task = fixture('request-task'); move(task,'cancelled'); move(task,'awaiting_approval')
        with self.assertRaisesRegex(Rejected, 'TRANSITION_FORBIDDEN'): validate_task(task)

    def test_unapproved_development(self):
        for state in ['implementing','review','done','approved']:
            task = fixture('request-task'); move(task,state)
            with self.subTest(state=state), self.assertRaises(Rejected): validate_task(task)

    def test_request_mutation_and_nonce(self):
        task = fixture('request-task'); task['approval_requests'][0]['context']['nonce'] = 'e'*32
        with self.assertRaisesRegex(Rejected,'CONTEXT_HASH'): validate_task(task)
        task = fixture('request-task'); task['approval_requests'] *= 2
        with self.assertRaisesRegex(Rejected,'NONCE_REUSED'): validate_task(task)

    def test_request_window_and_position(self):
        for key,value in [('not_after','2026-10-22T20:00:00Z'),('not_after','2026-10-21T12:00:00Z'),
                          ('history_position',1),('history_position',20)]:
            task = fixture('request-task'); q=task['approval_requests'][0]; q['context'][key]=value
            q['context_hash']=digest(q['context'])
            with self.subTest(key=key,value=value), self.assertRaises(Rejected): validate_task(task)

    def test_latest_request_supersedes(self):
        old=fixture('request-task'); task=copy.deepcopy(old)
        q=copy.deepcopy(task['approval_requests'][0]);q['context']['nonce']='e'*32;q['context_hash']=digest(q['context'])
        task['approval_requests'].append(q)
        validate_task(task,old)
        move(task,'approved',dict(run_id=10,nonce='f'*32,context_hash=task['approval_requests'][0]['context_hash']))
        with self.assertRaisesRegex(Rejected,'APPROVAL_CONTEXT_MISMATCH'): validate_task(task)
