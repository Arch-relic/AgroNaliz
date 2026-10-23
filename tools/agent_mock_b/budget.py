"""Budgets are derived from confirmed evidence; never refunded."""
from .canonical import require


def reservation(limits, runs, reserved):
    require(runs < limits['maxRunsPerTask'], 'BUDGET_EXHAUSTED')
    remaining = limits['maxTotalOutputBytesPerTask'] - reserved
    require(remaining > 0, 'BUDGET_EXHAUSTED')
    return min(limits['maxOutputBytes'], remaining), remaining


def action_limit(limits, count, elapsed_ms):
    require(count < limits['maxOperationsPerRun'], 'LIMIT_EXCEEDED')
    require(0 <= elapsed_ms <= limits['maxWallDurationMs'], 'LIMIT_EXCEEDED')
