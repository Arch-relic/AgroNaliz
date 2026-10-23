"""Trusted synthetic append-only approval records, not worker-provided input."""
import copy
from dataclasses import dataclass

from .canonical import require
from .contracts import REPOSITORY, approval, envelope, timestamp


@dataclass(frozen=True)
class Trust:
    expectedOwnerId: str
    expectedRepository: str = REPOSITORY


class ApprovalStore:
    def __init__(self):
        self._history = []
        self._logs = {}
        self.available = True

    def bind_log(self, task_digest, evidence):
        """One retained authoritative journal per taskDigest in this model world."""
        require(self.available, 'STORE_UNAVAILABLE')
        identity = (evidence.log_id, str(evidence.path.resolve()))
        require(self._logs.get(task_digest, identity) == identity, 'INTEGRITY_FAILED')
        self._logs[task_digest] = identity

    def put(self, record):
        require(self.available, 'STORE_UNAVAILABLE')
        approval(record)
        previous = [r for r in self._history if r['approvalId'] == record['approvalId']]
        if previous:
            last = previous[-1]
            require(all(record[k] == last[k] for k in record if k not in ('status', 'storeVersion')), 'INVALID_APPROVAL')
            require(last['status'] == 'ACTIVE' and record['status'] in ('REVOKED', 'DENIED'), 'INVALID_APPROVAL')
            require(record['storeVersion'] > last['storeVersion'], 'INVALID_APPROVAL')
        self._history.append(copy.deepcopy(record))

    def read(self, approval_id):
        require(self.available, 'STORE_UNAVAILABLE')
        for record in reversed(self._history):
            if record['approvalId'] == approval_id:
                return copy.deepcopy(record)
        require(False, 'INVALID_APPROVAL')

    def check(self, spec, task_digest, request, trust, now):
        require(type(trust) is Trust and type(trust.expectedOwnerId) is str and bool(trust.expectedOwnerId), 'STORE_UNAVAILABLE')
        require(trust.expectedRepository == spec['repository'] == REPOSITORY, 'INVALID_APPROVAL')
        envelope(request)
        record = self.read(request['approvalId'])
        approval(record)
        require(request['taskDigest'] == task_digest == record['taskDigest'], 'INVALID_APPROVAL')
        require(request['ownerId'] == record['ownerId'] == trust.expectedOwnerId, 'INVALID_APPROVAL')
        require(record['taskId'] == spec['taskId'] and record['revision'] == spec['revision'], 'INVALID_APPROVAL')
        require(sorted(record['scope']) == spec['allowedPaths'], 'INVALID_APPROVAL')
        require(record['status'] != 'REVOKED', 'APPROVAL_REVOKED')
        require(record['status'] == 'ACTIVE', 'INVALID_APPROVAL')
        current = timestamp(now)
        require(current >= timestamp(record['issuedAt']), 'INVALID_APPROVAL')
        require(current < timestamp(record['expiresAt']), 'APPROVAL_EXPIRED')
        return record
