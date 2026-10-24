"""Fresh-main authorization and trusted execution-context regression tests."""
import base64
import copy
import json
import unittest
from unittest.mock import patch

from agent_core.core import authorize, Denied, REPO, WORKFLOW, digest
import test_core as fixtures
move = fixtures.move


class RevocationTests(unittest.TestCase):
    def setUp(self):
        self.f = fixtures.CoreTests(); self.f.setUp()
        self.snapshot = copy.deepcopy(self.f.t)
        self.current = copy.deepcopy(self.snapshot)
        self.reads = []
        self.sha = 'b' * 40
        self.context = dict(id=99, repository={'full_name': REPO},
                            head_repository={'full_name': REPO}, head_branch='main',
                            head_sha='c'*40, event='workflow_dispatch', run_attempt=1,
                            status='in_progress', path=WORKFLOW, workflow_id=20,
                            actor={'id':123}, triggering_actor={'id':123})
        self.env = patch.dict('os.environ', GITHUB_REPOSITORY=REPO,
                              GITHUB_WORKFLOW_REF=f'{REPO}/{WORKFLOW}@refs/heads/main',
                              GITHUB_REF='refs/heads/main', GITHUB_RUN_ID='99',
                              GITHUB_RUN_ATTEMPT='1', GITHUB_SHA='c'*40,
                              GITHUB_ACTOR_ID='123',
                              GITHUB_EVENT_NAME='workflow_dispatch')
        self.env.start(); self.addCleanup(self.env.stop)
        self.mock = patch('agent_core.check.api', side_effect=self.api)
        self.mock.start(); self.addCleanup(self.mock.stop)

    def api(self, path):
        self.reads.append(path)
        if path == 'actions/runs/99': return self.context
        if path == 'git/ref/heads/main':
            return {'ref':'refs/heads/main','object':{'type':'commit','sha':self.sha}}
        if path == 'contents/agent_core/tasks/AGN-1.json?ref='+'b'*40:
            return {'type':'file', 'encoding':'base64', 'path':'agent_core/tasks/AGN-1.json',
                    'content':base64.encodebytes(json.dumps(self.current).encode()).decode()}
        return self.f.api(path)

    def test_current_approval(self):
        authorize(self.snapshot, 'implement')
        self.assertEqual(self.reads.count('git/ref/heads/main'), 2)
        self.assertIn('contents/agent_core/tasks/AGN-1.json?ref='+'b'*40, self.reads)
        self.assertFalse(any('?ref=main' in p for p in self.reads))

    def test_current_implementation_and_review(self):
        for state in ['implementing','review']:
            move(self.current,state)
            authorize(self.current,'technical_fix')

    def test_stale_after_revocation(self):
        for state in ['blocked', 'cancelled', 'awaiting_approval', 'implementing', 'review']:
            with self.subTest(state=state):
                self.current = copy.deepcopy(self.snapshot); move(self.current, state)
                with self.assertRaisesRegex(Denied, 'Stale task snapshot'): authorize(self.snapshot, 'implement')

    def test_new_revision(self):
        r = copy.deepcopy(self.current['revisions'][0]); r['spec']['id']='r2'
        r['hash']=digest(r['spec']); self.current['revisions'].append(r)
        self.current['current_revision']='r2'; move(self.current,'awaiting_approval')
        with self.assertRaises(Denied): authorize(self.snapshot,'implement')

    def test_main_changes_during_validation(self):
        def api(path):
            result = self.api(path)
            if path == 'actions/runs/10': self.sha = 'd'*40
            return result
        with patch('agent_core.check.api', side_effect=api), self.assertRaises(Denied):
            authorize(self.snapshot, 'implement')

    def test_api_failures_at_every_read(self):
        authorize(self.snapshot, 'implement'); reads=list(self.reads)
        for fail_at in range(len(reads)):
            counter = 0
            def api(path):
                nonlocal counter
                n=counter; counter+=1
                if n == fail_at: raise OSError('GitHub unavailable')
                return self.api(path)
            with self.subTest(read=fail_at), patch('agent_core.check.api', side_effect=api):
                with self.assertRaises(Denied): authorize(self.snapshot, 'implement')

    def test_missing_task(self):
        def api(path):
            if '?ref='+'b'*40 in path: raise FileNotFoundError('404')
            return self.api(path)
        with patch('agent_core.check.api', side_effect=api), self.assertRaises(Denied):
            authorize(self.snapshot,'implement')

    def test_executor_scope_and_history_tampering(self):
        for change in ['executor', 'scope', 'history', 'decisions']:
            with self.subTest(change=change):
                self.current=copy.deepcopy(self.snapshot)
                if change == 'executor': self.current['executor_id']=999
                elif change == 'scope':
                    r=self.current['revisions'][0]; r['spec']['operations'].append('room'); r['hash']=digest(r['spec'])
                elif change == 'history': self.current['history'][0]['reason']='different'
                else: self.current['links']['checks'].append('https://github.com/Arch-relic/AgroNaliz/actions/runs/1')
                with self.assertRaises(Denied): authorize(self.snapshot,'implement')

    def test_replayed_grant(self):
        move(self.current,'blocked'); move(self.current,'awaiting_approval'); move(self.current,'approved',10)
        with self.assertRaises(Denied): authorize(self.current,'implement')

    def test_matching_revoked_snapshot(self):
        for state in ['blocked','cancelled','awaiting_approval']:
            self.current=copy.deepcopy(self.snapshot); move(self.current,state)
            with self.subTest(state=state), self.assertRaises(Denied): authorize(self.current,'implement')

    def test_each_action_checks_again(self):
        authorize(self.snapshot,'implement'); move(self.current,'blocked')
        with self.assertRaises(Denied): authorize(self.snapshot,'technical_fix')

    def test_no_caller_supplied_identity_or_transport(self):
        for kwargs in [{'actor_id':123}, {'api':self.api}]:
            with self.subTest(kwargs=list(kwargs)), self.assertRaises(TypeError):
                authorize(self.snapshot,'implement',**kwargs)

    def test_untrusted_execution_metadata(self):
        for key, value in [('actor',{'id':999}), ('triggering_actor',{'id':999}),
                           ('head_branch','feature'), ('head_repository',{'full_name':'attacker/fork'}),
                           ('event','pull_request_target'), ('run_attempt',2),
                           ('status','completed'), ('path','other.yml'), ('workflow_id',21),
                           ('head_sha','d'*40)]:
            old=copy.deepcopy(self.context); self.context[key]=value
            with self.subTest(key=key), self.assertRaises(Denied): authorize(self.snapshot,'implement')
            self.context=old

    def test_untrusted_runner_context(self):
        for key,value in [('GITHUB_ACTOR_ID','999'),
                          ('GITHUB_WORKFLOW_REF',f'{REPO}/{WORKFLOW}@refs/heads/feature'),
                          ('GITHUB_REPOSITORY','attacker/fork'), ('GITHUB_REF','refs/heads/feature'),
                          ('GITHUB_RUN_ID','99/../../x'), ('GITHUB_RUN_ATTEMPT','2')]:
            with self.subTest(key=key), patch.dict('os.environ',{key:value}):
                with self.assertRaises(Denied): authorize(self.snapshot,'implement')

    def test_malformed_main_response(self):
        for result in [{}, {'ref':'refs/heads/feature','object':{'type':'commit','sha':'b'*40}},
                       {'ref':'refs/heads/main','object':{'type':'tag','sha':'b'*40}},
                       {'ref':'refs/heads/main','object':{'type':'commit','sha':'bad'}}]:
            def api(path): return result if path=='git/ref/heads/main' else self.api(path)
            with self.subTest(result=result), patch('agent_core.check.api',side_effect=api):
                with self.assertRaises(Denied): authorize(self.snapshot,'implement')

    def test_incomplete_or_ambiguous_task_response(self):
        valid=self.api('contents/agent_core/tasks/AGN-1.json?ref='+'b'*40)
        responses=[{}, dict(valid,type='symlink'), dict(valid,path='elsewhere.json'),
                   dict(valid,encoding='none'), dict(valid,content='!!'),
                   dict(valid,content=base64.b64encode(b'{"id":"AGN-1","id":"AGN-2"}').decode())]
        for result in responses:
            def api(path): return result if '?ref='+'b'*40 in path else self.api(path)
            with self.subTest(response=result), patch('agent_core.check.api',side_effect=api):
                with self.assertRaises(Denied): authorize(self.snapshot,'implement')
