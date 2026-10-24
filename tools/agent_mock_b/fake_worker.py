"""Synthetic streaming worker with a separate operation ledger, no I/O API."""
import copy

from .canonical import require


class FakeWorker:
    workerKind = 'mock'
    externalCalls = 0

    def __init__(self, response_factory, chunksize=256):
        self.response_factory = response_factory
        self.chunksize = chunksize
        self.workerStarts = 0
        self._ledger = {}
        self.available = True
        self.cancelConfirms = True
        self.cancelRequests = 0

    def stream(self, request, input_digest):
        require(self.available, 'STORE_UNAVAILABLE')
        operation_id = request['operationId']
        require(operation_id not in self._ledger, 'RECOVERY_REQUIRED')
        self.workerStarts += 1
        self._ledger[operation_id] = {'inputDigest': input_digest, 'state': 'STARTED'}
        raw = self.response_factory(copy.deepcopy(request))
        require(type(raw) is bytes, 'INVALID_SCHEMA')
        self._ledger[operation_id] = {'inputDigest': input_digest, 'state': 'COMPLETED', 'raw': raw}
        for offset in range(0, len(raw), self.chunksize):
            yield raw[offset:offset + self.chunksize]

    def lookup(self, operation_id):
        require(self.available, 'STORE_UNAVAILABLE')
        return copy.deepcopy(self._ledger.get(operation_id))

    def cancel(self, run_id):
        # Synthetic acknowledgment, not a statement about a real process.
        self.cancelRequests += 1
        return self.available and self.cancelConfirms
