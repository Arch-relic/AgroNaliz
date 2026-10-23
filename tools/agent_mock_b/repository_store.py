"""Independent synthetic base and immutable byte store."""
import copy

from .canonical import require
from .contracts import repository


class RepositoryStore:
    def __init__(self, state, snapshot):
        repository(state)
        self._state = copy.deepcopy(state)
        self._snapshot = snapshot
        self.available = True

    def read(self, spec, expected_repository):
        require(self.available, 'STORE_UNAVAILABLE')
        repository(self._state)
        require(self._state['repository'] == spec['repository'] == expected_repository, 'STALE_BASE')
        require(self._state['baseSha'] == spec['baseSha'], 'STALE_BASE')
        require(self._state['snapshotDigest'] == spec['snapshotDigest'] == self._snapshot.digest, 'SNAPSHOT_TAMPERED')
        return self._snapshot.verify()
