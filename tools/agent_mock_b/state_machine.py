"""Validate observable transitions, including named transient FSM states."""
from .canonical import require

TASK = {'PROPOSED': ('APPROVED', 'DENIED'), 'APPROVED': ('ACTIVE', 'REVOKED', 'EXPIRED'),
        'ACTIVE': ('REVIEWABLE', 'REVOKED', 'EXPIRED'),
        'REVIEWABLE': ('CLOSED', 'REVOKED', 'EXPIRED')}
RUN_PATH = ('CREATED', 'AUTHORIZED', 'INTENT_RECORDED', 'RUNNING', 'RESULT_RECEIVED',
            'VALIDATING', 'READY_FOR_OWNER_REVIEW')
BLOCKED = ('RECOVERY_REQUIRED', 'CANCELLATION_PENDING')
TERMINAL = ('READY_FOR_OWNER_REVIEW', 'FAILED', 'CANCELLED')


def task_transition(old, new):
    require(old == new or new in TASK.get(old, ()), 'INTEGRITY_FAILED')


def run_transition(old, new):
    if old == new:
        require(old not in TERMINAL + BLOCKED, 'INTEGRITY_FAILED')
        return
    require(old not in TERMINAL + BLOCKED, 'INTEGRITY_FAILED')
    if new in ('FAILED', 'CANCELLATION_PENDING', 'RECOVERY_REQUIRED', 'CANCELLED'):
        return
    # A durable event can span transient states, in the specified order.
    require(old in RUN_PATH and new in RUN_PATH and RUN_PATH.index(old) < RUN_PATH.index(new), 'INTEGRITY_FAILED')
