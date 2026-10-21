"""Offline-only controller model. Nothing imports this from the live v1 CLI."""
import re
from agent_core.v2.audit import Outcome
from agent_core.v2.approval import verify_approval
from agent_core.v2.credentials import CredentialBoundary, Lease
from agent_core.v2.model import Rejected, app_task, instant, need
from agent_core.v2.task import validate_task
from agent_core.v2.trust import bindings, installation, verify_run


class OfflineController:
    """Dependencies are a simulation harness, not a live authorization API.

    A fake can lie about everything; passing tests does not establish live trust.
    There is no production factory, command line, credential adapter or callback
    handler. A future live boundary requires independent G3–G5 approval.
    """

    def __init__(self, transport, clock, anchors, credentials=None):
        self.transport = transport
        self.clock = clock
        self.anchors = anchors
        self.credentials = credentials if credentials is not None else CredentialBoundary()

    def simulate(self, snapshot, operation, execution):
        lease, cleanup = None, 'not_run'
        outcome, reason, sha, task_id, context_hash = 'denied', 'TRUST_DATA_UNAVAILABLE', None, None, None
        expires, at = None, 'unavailable'
        try:
            start = self.clock.monotonic()
            current_time = self.clock.now()
            instant(current_time)
            at = current_time
            need(operation == 'test', 'OPERATION_FORBIDDEN')
            app_task(snapshot)
            task_id = snapshot['id']
            main = self.transport.read('main')
            need(type(main) is str and re.fullmatch('[a-f0-9]{40}', main), 'MAIN_REF_INVALID')
            sha = main
            task = self.transport.read(f'task/{task_id}@{sha}')
            need(task == snapshot, 'STALE_TASK_SNAPSHOT')
            registry = self.transport.read(f'registry@{sha}')
            policy = self.transport.read(f'policy@{sha}')
            bindings(registry, policy, self.anchors)
            need(task['executor'] == registry['executor'], 'EXECUTOR_BINDING_MISMATCH')
            need(type(execution) is dict and type(execution.get('run_id')) is int
                 and execution['run_id'] > 0 and type(execution.get('attempt')) is int
                 and type(execution.get('actor_id')) is int, 'EXECUTION_CONTEXT_MISMATCH')
            run = self.transport.read(f"run/{execution['run_id']}")
            need(type(run['id']) is int and run['id'] == execution['run_id'], 'RUN_ID_MISMATCH')
            verify_run(run, registry['controller_workflow'], self.anchors, approval=False)
            need(execution == {'run_id': run['id'], 'repository': self.anchors.repository_name,
                               'ref': 'refs/heads/main', 'sha': run['head_sha'],
                               'workflow_ref': f"{self.anchors.repository_name}/{run['path']}@refs/heads/main",
                               'event': 'workflow_dispatch', 'attempt': 1,
                               'actor_id': self.anchors.owner_id}, 'EXECUTION_CONTEXT_MISMATCH')
            request = validate_task(task, verify=lambda t, q, e, p:
                                    verify_approval(t, q, e, p, self.transport, registry, self.anchors))
            need(task['state'] in {'approved', 'implementing', 'review'} and request is not None,
                 'TASK_NOT_AUTHORIZED')
            ctx = request['context']
            context_hash = request['context_hash']
            revision = next(r for r in task['revisions'] if r['spec']['id'] == task['current_revision'])
            need(revision['spec']['operations'] == ['test'], 'OPERATION_FORBIDDEN')

            def time_check():
                now = instant(self.clock.now())
                server = instant(self.transport.read('server_time'))
                need(abs((now - server).total_seconds()) <= policy['clock_skew_seconds'], 'CLOCK_SKEW')
                need(instant(ctx['not_before']) <= now < instant(ctx['not_after']), 'CONSENT_NOT_CURRENT')
                elapsed = self.clock.monotonic() - start
                need(0 <= elapsed < policy['session_seconds'], 'SESSION_EXPIRED')
                need(now >= instant(run['created_at'])
                     and (now - instant(run['created_at'])).total_seconds() < policy['session_seconds'],
                     'CONTROLLER_RUN_EXPIRED')
                if lease is not None:
                    need(now < instant(lease.expires_at), 'LEASE_EXPIRED')

            time_check()
            installation(self.transport.read('installation'), registry)
            lease = self.credentials.acquire(installation_id=task['executor']['installation_id'],
                                             repository_ids=(self.anchors.repository_id,),
                                             permissions=dict(registry['permissions']))
            need(type(lease) is Lease, 'LEASE_INVALID')
            instant(lease.expires_at)
            expires = lease.expires_at
            need(lease.repository_ids == (self.anchors.repository_id,)
                 and lease.permissions == registry['permissions']
                 and type(lease.installation_id) is int
                 and lease.installation_id == task['executor']['installation_id'], 'LEASE_SCOPE_MISMATCH')
            # Recheck every action. No cached positive authorization or retry.
            installation(self.transport.read('installation'), registry)
            need(self.transport.read('main') == sha, 'MAIN_CHANGED')
            time_check()
            result = self.transport.read_identity_smoke(lease, self.anchors.repository_id)
            need(result == {'repository_id': self.anchors.repository_id, 'read_only': True}, 'SMOKE_RESULT_INVALID')
            outcome, reason = 'allowed', 'OFFLINE_SMOKE_VERIFIED'
        except Rejected as exc:
            reason = str(exc)
        except Exception:
            reason = 'TRUST_DATA_UNAVAILABLE'
        finally:
            if lease is not None:
                try:
                    self.credentials.revoke(lease)
                    cleanup = 'revoked'
                except BaseException:
                    cleanup, outcome, reason = 'failed', 'blocked', 'CLEANUP_FAILED'
        return Outcome(outcome, reason, at, sha, task_id, context_hash, expires, cleanup)
