"""fsync journal + independent checkpoint; unconfirmed tails block all actions."""
import threading
from pathlib import Path
from uuid import uuid4

from . import PROTOCOL
from .canonical import digest, dumps, loads, require
from .checkpoint import Checkpoint
from .contracts import (fields, hexstr, integer, uuid, timestamp, receipt, validation, string)

APPROVAL = ('APPROVAL_CHECKED', 'APPROVAL_DENIED', 'REVOKED', 'EXPIRED')
PROTECT = ('VALIDATION_REJECTED', 'STOP_REQUESTED', 'CANCEL_CONFIRMED', 'RECOVERY_REQUIRED')
SIMULATED = ('SIMULATED_PUBLISH_REQUESTED', 'SIMULATED_PUBLISHED')
EVENTS = APPROVAL + PROTECT + SIMULATED + ('INTENT', 'RESULT')
STATES_TASK = ('PROPOSED', 'APPROVED', 'ACTIVE', 'REVIEWABLE', 'CLOSED', 'DENIED', 'REVOKED', 'EXPIRED')
STATES_RUN = ('CREATED', 'AUTHORIZED', 'INTENT_RECORDED', 'RUNNING', 'RESULT_RECEIVED', 'VALIDATING',
              'READY_FOR_OWNER_REVIEW', 'FAILED', 'CANCELLED', 'CANCELLATION_PENDING', 'RECOVERY_REQUIRED')


def event_schema(event):
    fields(event, ('protocolVersion', 'logId', 'eventId', 'sequenceNo', 'taskId', 'runId',
                   'operationId', 'eventType', 'actor', 'timestamp', 'inputDigest', 'payload',
                   'outcome', 'previousEventHash', 'eventHash'))
    require(event['protocolVersion'] == PROTOCOL)
    for key in ('logId', 'eventId', 'taskId'):
        uuid(event[key])
    for key in ('runId', 'operationId'):
        if event[key] is not None:
            uuid(event[key])
    if event['inputDigest'] is not None:
        hexstr(event['inputDigest'])
    integer(event['sequenceNo'], 1); timestamp(event['timestamp'])
    for key in ('previousEventHash', 'eventHash'):
        hexstr(event[key])
    require(event['outcome'] in ('OK', 'REJECTED', 'UNKNOWN'))
    require(event['actor'] in ('controller', 'worker', 'validator', 'simulated-publisher'))
    kind, p = event['eventType'], event['payload']
    require(kind in EVENTS)
    if kind == 'INTENT':
        fields(p, ('kind', 'operationKind', 'operationId', 'inputDigest', 'taskDigest', 'approvalId',
                   'runId', 'fromTaskState', 'toTaskState', 'fromRunState', 'toRunState',
                   'reservedRunSlot', 'reservedOutputBytes', 'remainingTaskOutputBudgetBefore', 'baseSha', 'snapshotDigest'))
        uuid(p['approvalId']); hexstr(p['taskDigest']); hexstr(p['baseSha'], 40); hexstr(p['snapshotDigest'])
        for key in ('reservedRunSlot', 'reservedOutputBytes', 'remainingTaskOutputBudgetBefore'):
            integer(p[key])
        require(p['reservedRunSlot'] in (0, 1))
    elif kind == 'RESULT':
        fields(p, ('kind', 'operationKind', 'operationId', 'inputDigest', 'runId', 'fromTaskState',
                   'toTaskState', 'fromRunState', 'toRunState', 'actualOutputBytes', 'receipt',
                   'validationReceipt', 'workerResponseDigest'))
        integer(p['actualOutputBytes'])
        if p['receipt'] is not None:
            receipt(p['receipt'])
        if p['validationReceipt'] is not None:
            validation(p['validationReceipt'])
        if p['operationKind'] == 'WORKER' and event['outcome'] == 'OK':
            hexstr(p['workerResponseDigest'])
        else:
            require(p['workerResponseDigest'] is None)
        require((p['validationReceipt'] is not None) == (p['operationKind'] == 'VALIDATOR'))
    elif kind in APPROVAL:
        fields(p, ('kind', 'approvalId', 'taskDigest', 'ownerId', 'checkedAt', 'reason'))
        uuid(p['approvalId']); hexstr(p['taskDigest']); string(p['ownerId']); timestamp(p['checkedAt']); string(p['reason'])
    elif kind in PROTECT:
        fields(p, ('kind', 'runId', 'operationId', 'reason', 'confirmedStop'))
        require(type(p['confirmedStop']) is bool); string(p['reason'])
        for key in ('runId', 'operationId'):
            if p[key] is not None:
                uuid(p[key])
            require(p[key] == event[key])
        if kind == 'CANCEL_CONFIRMED':
            require(p['confirmedStop'] is True)
        else:
            require(p['confirmedStop'] is False)
    else:
        fields(p, ('kind', 'runId', 'validationEventSequenceNo', 'diffDigest', 'resultSnapshotDigest',
                   'simulated', 'externalEffects'))
        uuid(p['runId']); require(p['runId'] == event['runId']); integer(p['validationEventSequenceNo'], 1)
        hexstr(p['diffDigest']); hexstr(p['resultSnapshotDigest'])
        require(p['simulated'] is True and type(p['externalEffects']) is int and p['externalEffects'] == 0)
    require(p['kind'] == kind)
    if kind in ('INTENT', 'RESULT'):
        require(p['operationKind'] in ('WORKER', 'VALIDATOR', 'SIMULATED_PUBLISH'))
        for key in ('operationId', 'runId', 'inputDigest'):
            require(p[key] == event[key])
        uuid(p['runId']); uuid(p['operationId']); hexstr(p['inputDigest'])
        require(p['fromTaskState'] in STATES_TASK and p['toTaskState'] in STATES_TASK)
        require(p['fromRunState'] in STATES_RUN and p['toRunState'] in STATES_RUN)


class Evidence:
    def __init__(self, directory, task_id, log_id=None):
        directory = Path(directory)
        directory.mkdir(parents=True, exist_ok=True)
        self.task_id, self.log_id = task_id, log_id or str(uuid4())
        uuid(task_id); uuid(self.log_id)
        self.path = directory / 'evidence.jsonl'
        require(not self.path.exists(), 'INTEGRITY_FAILED')
        self.path.touch(exist_ok=False)
        self.lock = threading.RLock()
        self.available = self.readable = self.writable = True
        self.genesis = digest('genesis', {'logId': self.log_id, 'protocolVersion': PROTOCOL, 'taskId': task_id})
        self.checkpoint = Checkpoint(directory / 'checkpoint.json', {'logId': self.log_id,
            'sequenceNo': 0, 'rootHash': self.genesis, 'storeVersion': 0})

    def inspect(self):
        require(self.available and self.readable, 'STORE_UNAVAILABLE')
        cp = self.checkpoint.read()
        require(cp['logId'] == self.log_id, 'INTEGRITY_FAILED')
        raw = self.path.read_bytes()
        require(not raw or raw.endswith(b'\n'), 'RECOVERY_REQUIRED')
        rows = raw.splitlines()
        require(len(rows) >= cp['sequenceNo'], 'INTEGRITY_FAILED')
        root, confirmed, ids = self.genesis, [], set()
        for n, row in enumerate(rows[:cp['sequenceNo']], 1):
            event = loads(row); event_schema(event)
            require(event['logId'] == self.log_id and event['taskId'] == self.task_id
                    and event['sequenceNo'] == n and event['previousEventHash'] == root, 'INTEGRITY_FAILED')
            require(event['eventId'] not in ids, 'INTEGRITY_FAILED'); ids.add(event['eventId'])
            root = digest('evidence', {k: v for k, v in event.items() if k != 'eventHash'})
            require(root == event['eventHash'], 'INTEGRITY_FAILED')
            confirmed.append(event)
        require(root == cp['rootHash'], 'INTEGRITY_FAILED')
        return confirmed, cp, rows[cp['sequenceNo']:]

    def confirmed(self):
        events, _, tail = self.inspect()
        require(not tail, 'RECOVERY_REQUIRED')
        return events

    def append(self, kind, payload, now, run_id=None, operation_id=None, input_digest=None,
               actor='controller', outcome='OK', hook=lambda _: None, before_checkpoint=lambda: None):
        import os
        with self.lock:
            require(self.available and self.writable, 'STORE_UNAVAILABLE')
            _, old, tail = self.inspect()
            require(not tail, 'RECOVERY_REQUIRED')
            event = dict(protocolVersion=PROTOCOL, logId=self.log_id, eventId=str(uuid4()),
                sequenceNo=old['sequenceNo'] + 1, taskId=self.task_id, runId=run_id,
                operationId=operation_id, eventType=kind, actor=actor, timestamp=now,
                inputDigest=input_digest, payload=payload, outcome=outcome, previousEventHash=old['rootHash'])
            event['eventHash'] = digest('evidence', event)
            event_schema(event)
            with self.path.open('ab') as stream:
                stream.write(dumps(event) + b'\n'); stream.flush(); os.fsync(stream.fileno())
            hook(kind + ':append')
            before_checkpoint()
            self.checkpoint.compare_and_set(old, dict(logId=self.log_id, sequenceNo=event['sequenceNo'],
                rootHash=event['eventHash'], storeVersion=old['storeVersion'] + 1))
            hook(kind + ':checkpoint')
            return event
