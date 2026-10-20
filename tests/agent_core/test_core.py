import base64
import copy
import json
import unittest
from unittest.mock import patch
from agent_core.core import validate, authorize, digest, approval_title, Denied, WORKFLOW, REPO
from agent_core.check import run


def task():
    spec = dict(id='r1', title='Task', description='Scope', acceptance=['Tests pass'],
                operations=['implement','technical_fix','test','draft_pr'], constraints=['No protected edits'], risks=['Review'])
    return dict(schema_version=1,id='AGN-1',type='feature',state='proposed',
                issue='https://github.com/Arch-relic/AgroNaliz/issues/1',executor_id=123,
                current_revision='r1',revisions=[dict(spec=spec,hash=digest(spec))],
                history=[dict(**{'from':None},to='proposed',revision='r1',reason='Registered',
                              at='2026-10-20T00:00:00Z',approval_run=None)],decisions=[],
                links={k:[] for k in ['branches','commits','checks','prs']})


def move(t, state, approval=None):
    t['history'].append(dict(**{'from':t['state']},to=state,revision=t['current_revision'],
                             reason='Explicit reason',at='2026-10-20T00:00:00Z',approval_run=approval))
    t['state']=state


class CoreTests(unittest.TestCase):
    def setUp(self):
        self.t=task()
        move(self.t,'awaiting_approval')
        self.original=copy.deepcopy(self.t)
        move(self.t,'approved',10)
        self.receipt=dict(id=10,repository={'full_name':REPO},head_repository={'full_name':REPO},actor={'id':240867647},triggering_actor={'id':240867647},
                          event='workflow_dispatch',head_branch='main',run_attempt=1,status='completed',
                          conclusion='success',path=WORKFLOW,workflow_id=20,head_sha='a'*40,
                          display_title=approval_title(self.t,self.t['revisions'][0],2))

    def api(self,path):
        if path=='actions/runs/10': return self.receipt
        if path=='actions/workflows/agent-core.yml': return {'id':20}
        if path.startswith('contents/agent_core/tasks/AGN-1.json?ref='):
            return {'content':base64.b64encode(json.dumps(self.original).encode()).decode()}
        raise Denied('Unknown API request')

    def test_registration(self): validate(task())
    def test_valid_lifecycle(self):
        for state in ['implementing','review','done']: move(self.t,state)
        validate(self.t,api=self.api)
    def test_invalid_transition(self):
        t=task(); move(t,'done')
        with self.assertRaises(Denied): validate(t)
    def test_no_approval(self):
        with self.assertRaises(Denied): validate(self.t)
    def test_exact_approval(self): authorize(self.t,'implement',self.api,actor_id=123)
    def test_foreign_approver(self):
        self.receipt['actor']['id']=999
        with self.assertRaises(Denied): validate(self.t,api=self.api)
    def test_self_approval(self):
        self.t['executor_id']=240867647
        with self.assertRaises(Denied): validate(self.t,api=self.api)
    def test_forged_event(self):
        for key,value in [('event','issue_comment'),('head_branch','feature'),('run_attempt',2),('conclusion','failure'),('workflow_id',3),('path','other.yml'),('display_title','approved')]:
            with self.subTest(key=key):
                receipt=copy.deepcopy(self.receipt); self.receipt[key]=value
                with self.assertRaises(Denied): validate(self.t,api=self.api)
                self.receipt=receipt
    def test_changed_spec_hash(self):
        self.t['revisions'][0]['spec']['description']='Changed'
        with self.assertRaises(Denied): validate(self.t,api=self.api)
    def test_recomputed_hash(self):
        r=self.t['revisions'][0]; r['spec']['description']='Changed'; r['hash']=digest(r['spec'])
        with self.assertRaises(Denied): validate(self.t,api=self.api)
    def test_revision_revokes(self):
        r=copy.deepcopy(self.t['revisions'][0]); r['spec']['id']='r2'; r['hash']=digest(r['spec'])
        self.t['revisions'].append(r); self.t['current_revision']='r2'; move(self.t,'awaiting_approval')
        validate(self.t,api=self.api)
        with self.assertRaises(Denied): authorize(self.t,'implement',self.api,actor_id=123)
    def test_replay(self):
        move(self.t,'blocked'); move(self.t,'awaiting_approval'); move(self.t,'approved',10)
        with self.assertRaises(Denied): validate(self.t,api=self.api)
    def test_history_retained(self):
        previous=copy.deepcopy(self.original); self.t['history'][0]['reason']='Rewritten'
        with self.assertRaises(Denied): validate(self.t,previous,self.api)
    def test_decisions(self):
        self.t['decisions']=[dict(at='2026-10-20T00:00:00Z',options=['A','B'],selected='A',reason='Simpler',revision='r1')]
        validate(self.t,api=self.api)
        prev=copy.deepcopy(self.t); self.t['decisions']=[]
        with self.assertRaises(Denied): validate(self.t,prev,self.api)
    def test_protected(self):
        for op in ['room','backup','signing','production_policy','merge_main','publish_apk']:
            with self.subTest(op=op), self.assertRaises(Denied): authorize(self.t,op,self.api,actor_id=123)
    def test_missing_data(self):
        for key in task():
            t=task(); del t[key]
            with self.subTest(key=key), self.assertRaises(Denied): validate(t)
    def test_cancel_block(self):
        for state in ['blocked','cancelled']:
            t=copy.deepcopy(self.t); move(t,state); validate(t,api=self.api)
            with self.assertRaises(Denied): authorize(t,'implement',self.api,actor_id=123)
    def test_cancel_terminal(self):
        t=task(); move(t,'cancelled'); move(t,'awaiting_approval')
        with self.assertRaises(Denied): validate(t)
    def test_unknown_operation(self):
        with self.assertRaises(Denied): authorize(self.t,'invented',self.api,actor_id=123)
    def test_pr_cannot_dispatch(self):
        with patch.dict('os.environ',GITHUB_REPOSITORY=REPO,GITHUB_EVENT_NAME='pull_request',GITHUB_RUN_ATTEMPT='1'):
            with self.assertRaises(Denied): run('approve',{'inputs':{},'sender':{'id':240867647}})
    def test_schedule(self):
        with patch.dict('os.environ',GITHUB_REPOSITORY=REPO,GITHUB_EVENT_NAME='schedule',GITHUB_REF='refs/heads/main'):
            run('analysis',{'schedule':'17 6 * * 1'})
            with self.assertRaises(Denied): run('analysis',{'schedule':'* * * * *'})
    def test_workflow_safety(self):
        from pathlib import Path
        text=Path('.github/workflows/agent-core.yml').read_text()
        for forbidden in ['secrets.', 'contents: write', 'pull_request.head.sha', 'workflow_run:', 'eval ', 'git push']:
            self.assertNotIn(forbidden,text)
        self.assertIn('persist-credentials: false',text)
        self.assertIn('pull_request.base.sha',text)

if __name__=='__main__': unittest.main()
