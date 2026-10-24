"""Synthetic normalized evidence only. IDs and workflow paths are not live."""
import copy
from dataclasses import replace
from pathlib import Path
from agent_core.core import digest
from agent_core.v2.model import document
from agent_core.v2.credentials import CredentialBoundary, Lease
from agent_core.v2.trust import OfflineAnchors
from agent_core.v2.controller import OfflineController

FIXTURES = Path(__file__).parent / 'fixtures' / 'v2'


def fixture(name):
    return document((FIXTURES / (name + '.json')).read_bytes())


def move(task, state, approval=None):
    task['history'].append({'from': task['state'], 'to': state, 'revision': task['current_revision'],
                            'reason': 'Synthetic transition', 'at': '2026-10-21T12:00:10Z',
                            'approval': approval})
    task['state'] = state


class FakeClock:
    def __init__(self):
        self.utc, self.elapsed = '2026-10-21T12:00:20Z', 0

    def now(self):
        return self.utc

    def monotonic(self):
        return self.elapsed


class FakeCredentials(CredentialBoundary):
    def __init__(self):
        self.acquisitions, self.revocations = [], []
        self.lease = Lease(object(), '2026-10-21T13:00:00Z', (9004,),
                           fixture('registry')['permissions'], 9002)
        self.acquire_error = self.revoke_error = None

    def acquire(self, **kwargs):
        self.acquisitions.append(copy.deepcopy(kwargs))
        if self.acquire_error:
            raise self.acquire_error
        return self.lease

    def revoke(self, lease):
        self.revocations.append(lease)
        if self.revoke_error:
            raise self.revoke_error


class FakeTransport:
    def __init__(self, records):
        self.records = records
        self.calls, self.operations = [], []
        self.fault_at, self.hook, self.smoke_error = None, None, None
        self.error = OSError('CANARY-DO-NOT-LOG')

    def read(self, resource):
        self.calls.append(resource)
        if self.hook:
            self.hook(self, resource)
        if len(self.calls) == self.fault_at:
            raise self.error
        return copy.deepcopy(self.records[resource])

    def read_identity_smoke(self, lease, repository_id):
        self.operations.append(repository_id)
        if self.smoke_error:
            raise self.smoke_error
        return {'repository_id': repository_id, 'read_only': True}


class Harness:
    def __init__(self):
        self.original = fixture('request-task')
        self.task = copy.deepcopy(self.original)
        request = self.task['approval_requests'][0]
        move(self.task, 'approved', {'run_id': 9010, 'nonce': request['context']['nonce'],
                                    'context_hash': request['context_hash']})
        self.registry, self.policy = fixture('registry'), fixture('policy')
        self.anchors = OfflineAnchors(9004, 'synthetic/fixture', 240867647,
                                      digest(self.registry), digest(self.policy))
        self.clock, self.credentials = FakeClock(), FakeCredentials()
        self.records = {'main': 'b'*40, 'task/AGN-9001@'+'b'*40: self.task,
                        'task/AGN-9001@'+'a'*40: self.original,
                        'registry@'+'b'*40: self.registry, 'policy@'+'b'*40: self.policy,
                        'server_time': self.clock.utc,
                        'installation': dict(app_id=9001, installation_id=9002,
                                             installation_account_id=9003, suspended=False, active=True,
                                             repository_selection='selected', repository_ids=[9004],
                                             complete=True, permissions=self.registry['permissions'])}
        for approval, run_id, sha in [(True, 9010, 'a'*40), (False, 9011, 'b'*40)]:
            workflow = self.registry['approval_workflow' if approval else 'controller_workflow']
            self.records[f'run/{run_id}'] = dict(
                id=run_id, repository={'id':9004, 'full_name':'synthetic/fixture'},
                head_repository={'id':9004, 'full_name':'synthetic/fixture'},
                actor={'id':240867647, 'type':'User'}, triggering_actor={'id':240867647, 'type':'User'},
                event='workflow_dispatch', head_branch='main', run_attempt=1,
                path=workflow['path'], workflow_id=workflow['id'], head_sha=sha,
                source_sha=workflow['source_sha'], source_hash=workflow['source_hash'],
                status='completed' if approval else 'in_progress', conclusion='success' if approval else None,
                created_at='2026-10-21T12:00:01Z', updated_at='2026-10-21T12:00:09Z',
                display_title=f"approve-v2:AGN-9001:{request['context_hash']}" if approval else 'offline controller')
        self.transport = FakeTransport(self.records)
        self.execution = dict(run_id=9011, repository='synthetic/fixture', ref='refs/heads/main',
                              sha='b'*40, workflow_ref='synthetic/fixture/.github/workflows/offline-controller.yml@refs/heads/main',
                              event='workflow_dispatch', attempt=1, actor_id=240867647)

    def run(self, operation='test'):
        return OfflineController(self.transport, self.clock, self.anchors, self.credentials).simulate(
            self.task, operation, self.execution)
