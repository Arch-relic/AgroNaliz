"""Independent synthetic checkpoint adapter with a monotonic trusted anchor.

The anchor is synthetic trust, not an OS or multiprocess tamper-proof guarantee.
Reopening a log requires the same independently retained adapter/anchor.
"""
import copy
import os
from pathlib import Path

from .canonical import dumps, loads, require
from .contracts import fields, hexstr, integer, uuid


class Checkpoint:
    def __init__(self, path, initial):
        self.path = Path(path)
        require(not self.path.exists(), 'INTEGRITY_FAILED')
        self.available = self.readable = self.writable = True
        self._anchor = None
        self._write(initial)

    def _write(self, value):
        require(self.available and self.writable, 'INTEGRITY_UNVERIFIED')
        fields(value, ('logId', 'sequenceNo', 'rootHash', 'storeVersion'))
        uuid(value['logId']); integer(value['sequenceNo']); integer(value['storeVersion']); hexstr(value['rootHash'])
        temporary = self.path.with_suffix('.pending')
        with temporary.open('xb') as stream:
            stream.write(dumps(value)); stream.flush(); os.fsync(stream.fileno())
        os.replace(temporary, self.path)
        descriptor = os.open(self.path.parent, os.O_RDONLY | os.O_DIRECTORY)
        try:
            os.fsync(descriptor)
        finally:
            os.close(descriptor)
        self._anchor = copy.deepcopy(value)

    def read(self):
        require(self.available and self.readable, 'INTEGRITY_UNVERIFIED')
        require(self.path.exists(), 'INTEGRITY_UNVERIFIED')
        value = loads(self.path.read_bytes())
        require(value == self._anchor, 'INTEGRITY_FAILED')
        return copy.deepcopy(value)

    def compare_and_set(self, old, new):
        require(self.read() == old, 'INTEGRITY_FAILED')
        require(new['logId'] == old['logId'] and new['sequenceNo'] == old['sequenceNo'] + 1
                and new['storeVersion'] == old['storeVersion'] + 1, 'INTEGRITY_FAILED')
        self._write(new)
