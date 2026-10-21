"""Allowlisted offline outcomes: never include raw exceptions/credentials."""
from dataclasses import dataclass


@dataclass(frozen=True)
class Outcome:
    outcome: str
    reason: str
    at: str
    main_sha: str | None = None
    task_id: str | None = None
    context_hash: str | None = None
    token_expires_at: str | None = None
    cleanup: str = 'not_run'
    offline: bool = True
