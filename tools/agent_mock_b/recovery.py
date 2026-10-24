"""Replay solely from the independently checkpointed evidence prefix."""
from dataclasses import dataclass, field

from .canonical import require
from .state_machine import task_transition, run_transition, TERMINAL, BLOCKED


@dataclass
class Projection:
    tasks: dict = field(default_factory=dict)
    runs: dict = field(default_factory=dict)
    budgets: dict = field(default_factory=dict)
    pending: dict = field(default_factory=dict)
    receipts: dict = field(default_factory=dict)
    worker_results: dict = field(default_factory=dict)
    validations: dict = field(default_factory=dict)
    events: list = field(default_factory=list)


def replay(evidence, allow_tail=False):
    events, _, tail = evidence.inspect()
    require(not tail or allow_tail, 'RECOVERY_REQUIRED')
    projection = Projection(events=events)
    operations = set()
    for event in events:
        p, kind = event['payload'], event['eventType']
        run_id = event['runId']
        if kind in ('APPROVAL_CHECKED', 'APPROVAL_DENIED', 'REVOKED', 'EXPIRED'):
            pair = (p['taskDigest'], p['approvalId'])
            old = projection.tasks.get(pair, 'PROPOSED')
            if kind == 'APPROVAL_CHECKED':
                require(old not in ('DENIED', 'REVOKED', 'EXPIRED', 'CLOSED'), 'INTEGRITY_FAILED')
                if old == 'PROPOSED':
                    projection.tasks[pair] = 'APPROVED'
            else:
                new = {'APPROVAL_DENIED': 'DENIED', 'REVOKED': 'REVOKED', 'EXPIRED': 'EXPIRED'}[kind]
                if kind == 'APPROVAL_DENIED':
                    require(old == 'PROPOSED', 'INTEGRITY_FAILED')
                else:
                    task_transition(old, new)
                projection.tasks[pair] = new
        elif kind == 'INTENT':
            op_id = p['operationId']
            require(op_id not in operations, 'INTEGRITY_FAILED'); operations.add(op_id)
            pair = (p['taskDigest'], p['approvalId'])
            old_task = projection.tasks.get(pair, 'PROPOSED')
            require(old_task == p['fromTaskState'], 'INTEGRITY_FAILED')
            task_transition(old_task, p['toTaskState'])
            require(p['toTaskState'] in ('ACTIVE', 'REVIEWABLE'), 'INTEGRITY_FAILED')
            if p['operationKind'] == 'WORKER':
                require(run_id not in projection.runs and p['reservedRunSlot'] == 1
                        and 0 < p['reservedOutputBytes'] <= p['remainingTaskOutputBudgetBefore'], 'INTEGRITY_FAILED')
                projection.runs[run_id] = dict(state='CREATED', pair=pair, operations=0,
                    reserve=p['reservedOutputBytes'], baseSha=p['baseSha'], snapshotDigest=p['snapshotDigest'])
                budget = projection.budgets.setdefault(p['taskDigest'], {'runs': 0, 'bytes': 0})
                budget['runs'] += 1; budget['bytes'] += p['reservedOutputBytes']
            else:
                require(run_id in projection.runs and p['reservedRunSlot'] == p['reservedOutputBytes'] == 0, 'INTEGRITY_FAILED')
                require(not any(x['runId'] == run_id for x in projection.pending.values()), 'INTEGRITY_FAILED')
                require(run_id in projection.worker_results, 'INTEGRITY_FAILED')
                if p['operationKind'] == 'SIMULATED_PUBLISH':
                    require(run_id in projection.validations, 'INTEGRITY_FAILED')
            run = projection.runs[run_id]
            require(run['pair'] == pair and run['state'] == p['fromRunState']
                    and run['baseSha'] == p['baseSha'] and run['snapshotDigest'] == p['snapshotDigest'], 'INTEGRITY_FAILED')
            run_transition(run['state'], p['toRunState'])
            run['state'] = p['toRunState']; run['operations'] += 1
            projection.tasks[pair] = p['toTaskState']
            projection.pending[op_id] = p
        elif kind == 'RESULT':
            op_id = p['operationId']
            require(op_id in projection.pending, 'INTEGRITY_FAILED')
            intent = projection.pending[op_id]
            require(all(p[k] == intent[k] for k in ('operationKind', 'operationId', 'inputDigest', 'runId')), 'INTEGRITY_FAILED')
            run = projection.runs[run_id]
            pair = run['pair']
            require(run['state'] == p['fromRunState'] and projection.tasks[pair] == p['fromTaskState'], 'INTEGRITY_FAILED')
            task_transition(p['fromTaskState'], p['toTaskState']); run_transition(run['state'], p['toRunState'])
            require(event['actor'] == {'WORKER': 'worker', 'VALIDATOR': 'validator', 'SIMULATED_PUBLISH': 'simulated-publisher'}[p['operationKind']], 'INTEGRITY_FAILED')
            r = p['receipt']
            require(r is not None and r['taskId'] == event['taskId'] and r['runId'] == run_id
                    and r['operationId'] == op_id and r['inputDigest'] == p['inputDigest']
                    and r['phase'] == p['operationKind'] and r['outcome'] == event['outcome'], 'INTEGRITY_FAILED')
            if 'evidenceSequenceNo' in r:
                require(r['evidenceSequenceNo'] == event['sequenceNo'], 'INTEGRITY_FAILED')
            if p['operationKind'] == 'WORKER':
                require(p['actualOutputBytes'] <= run['reserve'], 'INTEGRITY_FAILED')
                if event['outcome'] == 'OK':
                    require(p['toRunState'] == 'RESULT_RECEIVED', 'INTEGRITY_FAILED')
                    projection.worker_results[run_id] = event
            else:
                require(p['actualOutputBytes'] == 0, 'INTEGRITY_FAILED')
            if p['operationKind'] == 'VALIDATOR':
                v = p['validationReceipt']; w = projection.worker_results[run_id]
                require(v['workerResponseDigest'] == w['payload']['workerResponseDigest']
                        and v['runId'] == run_id and v['operationId'] == op_id
                        and v['taskDigest'] == pair[0] and v['taskId'] == event['taskId']
                        and v['baseSha'] == run['baseSha'] and v['snapshotDigest'] == run['snapshotDigest'], 'INTEGRITY_FAILED')
                require((v['validationStatus'] == 'PASSED') == (event['outcome'] == 'OK'), 'INTEGRITY_FAILED')
                projection.validations[run_id] = event
            if p['operationKind'] == 'SIMULATED_PUBLISH':
                require(event['outcome'] == 'OK' and run.get('published') == op_id
                        and p['toRunState'] == 'READY_FOR_OWNER_REVIEW' and p['toTaskState'] == 'REVIEWABLE', 'INTEGRITY_FAILED')
            projection.receipts[op_id] = r
            del projection.pending[op_id]
            run['state'] = p['toRunState']; projection.tasks[pair] = p['toTaskState']
        elif kind in ('SIMULATED_PUBLISH_REQUESTED', 'SIMULATED_PUBLISHED'):
            op_id = event['operationId']
            require(op_id in projection.pending and projection.pending[op_id]['operationKind'] == 'SIMULATED_PUBLISH', 'INTEGRITY_FAILED')
            run = projection.runs[run_id]
            v = projection.validations[run_id]
            vr = v['payload']['validationReceipt']
            require(v['sequenceNo'] == p['validationEventSequenceNo'] and vr['validationStatus'] == 'PASSED'
                    and p['diffDigest'] == vr['diffDigest'] and p['resultSnapshotDigest'] == vr['resultSnapshotDigest'], 'INTEGRITY_FAILED')
            if kind == 'SIMULATED_PUBLISH_REQUESTED':
                require('requested' not in run, 'INTEGRITY_FAILED'); run['requested'] = op_id
            else:
                require(run.get('requested') == op_id and 'published' not in run, 'INTEGRITY_FAILED'); run['published'] = op_id
        elif kind in ('STOP_REQUESTED', 'CANCEL_CONFIRMED', 'RECOVERY_REQUIRED', 'VALIDATION_REJECTED'):
            if run_id is not None:
                require(run_id in projection.runs, 'INTEGRITY_FAILED')
                run = projection.runs[run_id]
                new = {'STOP_REQUESTED': 'CANCELLATION_PENDING', 'CANCEL_CONFIRMED': 'CANCELLED',
                       'RECOVERY_REQUIRED': 'RECOVERY_REQUIRED', 'VALIDATION_REJECTED': 'FAILED'}[kind]
                require(run['state'] not in TERMINAL, 'INTEGRITY_FAILED')
                if run['state'] in BLOCKED:
                    require(new in ('FAILED', 'CANCELLED'), 'INTEGRITY_FAILED')
                run['state'] = new
    return projection


def recovery_decision(evidence, worker):
    """Read-only reconciliation proposal. Never silently accepts/discards a tail."""
    projection = replay(evidence, allow_tail=True)
    _, _, tail = evidence.inspect()
    if tail:
        return {'state': 'RECOVERY_REQUIRED', 'tailEvents': len(tail), 'operations': {}}
    operations = {}
    for op_id, intent in projection.pending.items():
        ledger = worker.lookup(op_id)
        # No ledger record cannot prove an action did not happen.
        operations[op_id] = 'RESULT_AVAILABLE' if ledger is not None else 'OUTCOME_UNKNOWN'
    return {'state': 'RECOVERY_REQUIRED' if operations else 'VERIFIED', 'tailEvents': 0, 'operations': operations}
