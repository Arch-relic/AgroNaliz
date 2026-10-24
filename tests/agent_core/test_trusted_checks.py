"""Synthetic GitHub API replay: trusted code consumes candidate data only."""
import base64
import copy
import json
import unittest
from unittest.mock import patch
from agent_core.core import Denied, REPO, load_json, validate, authorize, digest
from agent_core.check import run
import test_core as fixtures
task = fixtures.task
move = fixtures.move


class TrustedChecks(unittest.TestCase):
    def setUp(self):
        self.old=task(); self.new=copy.deepcopy(self.old)
        self.files=[]
        self.event={'pull_request':{'number':12,'base':{'repo':{'full_name':REPO},'ref':'main','sha':'a'*40},
                                    'head':{'sha':'b'*40},'changed_files':0}}
        self.env=patch.dict('os.environ',GITHUB_REPOSITORY=REPO,GITHUB_EVENT_NAME='pull_request_target',GITHUB_RUN_ATTEMPT='1')
        self.env.start(); self.addCleanup(self.env.stop)
        self.mock=patch('agent_core.check.api',side_effect=self.api); self.mock.start(); self.addCleanup(self.mock.stop)

    def api(self,path):
        if path == 'pulls/12': return self.event['pull_request']
        if path.startswith('contents/agent_core/tasks?'):
            return [{'name':'AGN-1.json','path':'agent_core/tasks/AGN-1.json','type':'file'}]
        if path.startswith('contents/agent_core/recommendations?'): return []
        if path.startswith('contents/agent_core/tasks/AGN-1.json?ref='):
            t=self.old if path.endswith('a'*40) else self.new
            return {'type':'file','encoding':'base64','content':base64.b64encode(json.dumps(t).encode()).decode()}
        if path.startswith('pulls/12/files?'): return self.files
        raise Denied('Unexpected API request: '+path)

    def changed(self,*files):
        self.files=[{'filename':p} for p in files]; self.event['pull_request']['changed_files']=len(files)

    def test_valid_registration_pr(self): run('pr',self.event)
    def test_valid_transition_pr(self):
        move(self.new,'awaiting_approval'); self.changed('agent_core/tasks/AGN-1.json'); run('pr',self.event)
    def test_pr_forged_grant(self):
        move(self.new,'awaiting_approval'); move(self.new,'approved',10)
        with self.assertRaises(Denied): run('pr',self.event)
    def test_candidate_schema_cannot_relax_gate(self):
        self.changed('agent_core/schemas/task-v1.json')
        with self.assertRaises(Denied): run('pr',self.event)
    def test_candidate_workflow_rejected(self):
        self.changed('.github/workflows/agent-core.yml')
        with self.assertRaises(Denied): run('pr',self.event)
    def test_package_init_cannot_inject(self):
        self.changed('agent_core/__init__.py')
        with self.assertRaises(Denied): run('pr',self.event)
    def test_rename_cannot_hide_governance_change(self):
        self.changed('innocent.txt'); self.files[0]['previous_filename']='agent_core/core.py'
        with self.assertRaises(Denied): run('pr',self.event)
    def test_incomplete_diff_denied(self):
        self.event['pull_request']['changed_files']=1
        with self.assertRaises(Denied): run('pr',self.event)
    def test_history_change_denied(self):
        self.new['history'][0]['reason']='Rewritten'
        with self.assertRaises(Denied): run('pr',self.event)
    def test_api_outage_fails_closed(self):
        with patch('agent_core.check.api',side_effect=OSError('Offline')):
            with self.assertRaises(OSError): run('pr',self.event)
    def test_duplicate_json_keys(self):
        with self.assertRaises(Denied): load_json('{"state":"approved","state":"proposed"}')
    def test_nonfinite_json(self):
        with self.assertRaises(Denied): load_json('{"n":NaN}')
    def test_wrong_repo(self):
        self.event['pull_request']['base']['repo']['full_name']='attacker/fork'
        with self.assertRaises(Denied): run('pr',self.event)


class AdditionalApprovalTests(unittest.TestCase):
    def setUp(self):
        self.fixture=fixtures.CoreTests(); self.fixture.setUp()
    def test_approval_history_is_bound(self):
        self.fixture.t['history'][0]['reason']='Alternative history'
        with self.assertRaises(Denied): validate(self.fixture.t,api=self.fixture.api)
    def test_issue_cannot_rebind(self):
        self.fixture.t['issue']='https://github.com/Arch-relic/AgroNaliz/issues/2'
        with self.assertRaises(Denied): validate(self.fixture.t,api=self.fixture.api)
    def test_executor_identity_required(self):
        for identity in [None,999]:
            with self.subTest(identity=identity), self.assertRaises(Denied):
                fixtures.authorize_fixture(self.fixture.t,'implement',self.fixture.api,actor_id=identity)
    def test_injected_operation_requires_reapproval(self):
        r=self.fixture.t['revisions'][0]; r['spec']['operations'].append('room'); r['hash']=digest(r['spec'])
        with self.assertRaises(Denied): validate(self.fixture.t,api=self.fixture.api)
    def test_backward_timestamp(self):
        self.fixture.t['history'][-1]['at']='2026-10-19T00:00:00Z'
        with self.assertRaises(Denied): validate(self.fixture.t,api=self.fixture.api)
    def test_head_repository(self):
        self.fixture.receipt['head_repository']['full_name']='attacker/fork'
        with self.assertRaises(Denied): validate(self.fixture.t,api=self.fixture.api)


class DispatchTests(unittest.TestCase):
    def setUp(self):
        from agent_core.core import approval_title
        self.t=task(); move(self.t,'awaiting_approval')
        r=self.t['revisions'][0]
        self.event={'sender':{'id':240867647},'inputs':{'mode':'approve','task':'AGN-1',
                    'spec_hash':r['hash'],'operations_hash':digest(r['spec']['operations']),
                    'executor_id':'123','sequence':str(len(self.t['history']))}}
        self.env=patch.dict('os.environ',GITHUB_REPOSITORY=REPO,GITHUB_EVENT_NAME='workflow_dispatch',
                            GITHUB_RUN_ATTEMPT='1',GITHUB_REF='refs/heads/main',GITHUB_SHA='a'*40)
        self.env.start(); self.addCleanup(self.env.stop)
        self.mock=patch('agent_core.check.document',return_value=self.t); self.mock.start(); self.addCleanup(self.mock.stop)
    def test_valid_owner_dispatch(self): run('approve',self.event)
    def test_dispatch_input_tampering(self):
        for key in ['spec_hash','operations_hash','executor_id','sequence']:
            event=copy.deepcopy(self.event); event['inputs'][key]='fake'
            with self.subTest(key=key),self.assertRaises(Denied): run('approve',event)
    def test_foreign_dispatch(self):
        self.event['sender']['id']=999
        with self.assertRaises(Denied): run('approve',self.event)
    def test_branch_dispatch(self):
        with patch.dict('os.environ',GITHUB_REF='refs/heads/attacker'):
            with self.assertRaises(Denied): run('approve',self.event)
    def test_rerun_dispatch(self):
        with patch.dict('os.environ',GITHUB_RUN_ATTEMPT='2'):
            with self.assertRaises(Denied): run('approve',self.event)
    def test_session_start_contract(self):
        self.event['inputs']={'mode':'session_start'}
        run('analysis',self.event)
    def test_unknown_analysis_mode(self):
        self.event['inputs']={'mode':'wrong'}
        with self.assertRaises(Denied): run('analysis',self.event)


class ContractTests(unittest.TestCase):
    def test_schema_definitions(self):
        from pathlib import Path
        from jsonschema import Draft202012Validator
        for path in Path('agent_core/schemas').glob('*.json'):
            Draft202012Validator.check_schema(load_json(path.read_text()))
    def test_invalid_recommendation(self):
        from agent_core.core import schema
        with self.assertRaises(Denied): schema({'schema_version':1,'status':'approved'},'recommendation-v1.json')
    def test_local_fake_approval_not_authority(self):
        t=task(); move(t,'awaiting_approval'); move(t,'approved',10)
        with self.assertRaises(Denied): validate(t)
