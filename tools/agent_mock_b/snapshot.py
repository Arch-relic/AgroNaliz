"""Immutable regular UTF-8 snapshots; never read the real working tree."""
from types import MappingProxyType

from .canonical import digest, sha, require, Refusal
from .contracts import path


def text(raw):
    require(type(raw) is bytes, 'INVALID_UTF8')
    require(not raw.startswith(b'\xef\xbb\xbf') and b'\0' not in raw, 'INVALID_UTF8')
    try:
        return raw.decode('utf-8', errors='strict')
    except UnicodeError:
        raise Refusal('INVALID_UTF8') from None


class Snapshot:
    def __init__(self, files):
        require(type(files) is dict, 'INVALID_SCHEMA')
        folded = set()
        for name, raw in files.items():
            path(name); text(raw)
            require(name.lower() not in folded, 'INVALID_PATH')
            folded.add(name.lower())
        # No file may also be the parent directory of another file.
        for name in files:
            parts = name.split('/')
            require(not any('/'.join(parts[:i]).lower() in folded for i in range(1, len(parts))), 'INVALID_PATH')
        self.files = MappingProxyType(dict(files))
        self._manifest = tuple((name, len(raw), sha(raw)) for name, raw in sorted(files.items()))
        self.digest = digest('snapshot', self.manifest())

    def manifest(self):
        return [{'path': p, 'byteLength': n, 'sha256': h, 'mode': '100644',
                 'contentType': 'text/plain; charset=utf-8'} for p, n, h in self._manifest]

    def verify(self):
        try:
            current = Snapshot(dict(self.files))
        except Refusal:
            raise Refusal('SNAPSHOT_TAMPERED') from None
        require(current.manifest() == self.manifest() and current.digest == self.digest, 'SNAPSHOT_TAMPERED')
        return self
