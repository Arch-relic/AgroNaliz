"""Offline contract only: no PEM, signing, HTTP, environment or token parsing."""
from dataclasses import dataclass, field
from agent_core.v2.model import Rejected


@dataclass(frozen=True)
class Lease:
    handle: object = field(repr=False)
    expires_at: str
    repository_ids: tuple
    permissions: dict
    installation_id: int


class CredentialBoundary:
    """Unconfigured by default. Only synthetic tests supply an implementation."""

    def acquire(self, *, installation_id, repository_ids, permissions):
        raise Rejected('LIVE_CREDENTIALS_DISABLED')

    def revoke(self, lease):
        raise Rejected('LIVE_CREDENTIALS_DISABLED')
