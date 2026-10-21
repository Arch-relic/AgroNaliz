"""Append-only v2 state machine; no migration or live task writes."""
from agent_core.core import EDGES, digest
from agent_core.v2.model import app_task, instant, need


def validate_task(task, previous=None, verify=None):
    app_task(task)
    if previous is not None:
        app_task(previous)
        for key in ('id', 'issue', 'type', 'executor', 'predecessor_task_id'):
            need(task.get(key) == previous.get(key), 'IMMUTABLE_IDENTITY_CHANGED')
        for key in ('revisions', 'history', 'decisions', 'approval_requests'):
            need(task[key][:len(previous[key])] == previous[key], 'APPEND_ONLY_VIOLATION')
        if len(task['approval_requests']) > len(previous['approval_requests']):
            need(task['state'] == 'awaiting_approval', 'REQUEST_OUTSIDE_AWAITING')
            for request in task['approval_requests'][len(previous['approval_requests']):]:
                need(request['context']['history_position'] == len(task['history']),
                     'REQUEST_POSITION_MISMATCH')
    need(task.get('predecessor_task_id') != task['id'], 'SELF_PREDECESSOR')
    revisions = {r['spec']['id']: r for r in task['revisions']}
    need(len(revisions) == len(task['revisions']), 'DUPLICATE_REVISION')
    for revision in revisions.values():
        need(revision['hash'] == digest(revision['spec']), 'SPEC_HASH_MISMATCH')
    requests, nonces, last_position = {}, set(), 0
    for request in task['approval_requests']:
        ctx = request['context']
        pos = ctx['history_position']
        need(pos >= last_position and pos <= len(task['history']), 'REQUEST_POSITION_MISMATCH')
        last_position = pos
        need(ctx['nonce'] not in nonces, 'REQUEST_NONCE_REUSED')
        nonces.add(ctx['nonce'])
        need(request['context_hash'] == digest(ctx), 'CONTEXT_HASH_MISMATCH')
        need(ctx['task_id'] == task['id'] and ctx['executor'] == task['executor']
             and ctx['repository_id'] == task['executor']['repository_id']
             and ctx['policy_version'] == task['executor']['policy_version'], 'CONTEXT_IDENTITY_MISMATCH')
        rid = ctx['revision_id']
        need(rid in revisions and ctx['spec_hash'] == revisions[rid]['hash']
             and ctx['operations_hash'] == digest(revisions[rid]['spec']['operations']), 'CONTEXT_SPEC_MISMATCH')
        duration = (instant(ctx['not_after']) - instant(ctx['not_before'])).total_seconds()
        need(0 < duration <= 28800, 'CONSENT_DURATION_INVALID')
        prefix = task['history'][:pos]
        need(prefix and prefix[-1]['to'] == 'awaiting_approval'
             and prefix[-1]['revision'] == rid, 'REQUEST_OUTSIDE_AWAITING')
        requests[pos] = request
    state, active, last_revision, last_time = None, None, None, None
    used_runs, used_nonces = set(), set()
    for position, event in enumerate(task['history']):
        target, rid = event['to'], event['revision']
        at = instant(event['at'])
        need(last_time is None or at >= last_time, 'HISTORY_TIME_ORDER')
        last_time = at
        need(rid in revisions and event['from'] == state, 'HISTORY_CONTRADICTION')
        need((state is None and position == 0 and target == 'proposed')
             or (state is not None and target in EDGES[state]), 'TRANSITION_FORBIDDEN')
        if rid != last_revision:
            need(target in {'proposed', 'awaiting_approval'}, 'REVISION_REQUIRES_APPROVAL')
            active = None
        if target == 'approved':
            evidence = event['approval']
            request = requests.get(position)
            need(evidence is not None and request is not None, 'APPROVAL_REQUIRED')
            ctx = request['context']
            need(evidence['nonce'] == ctx['nonce'] and evidence['context_hash'] == request['context_hash']
                 and ctx['revision_id'] == rid, 'APPROVAL_CONTEXT_MISMATCH')
            need(evidence['run_id'] not in used_runs and evidence['nonce'] not in used_nonces,
                 'APPROVAL_REPLAY')
            need(verify is not None, 'INDEPENDENT_EVIDENCE_REQUIRED')
            verify(task, request, event, position)
            used_runs.add(evidence['run_id'])
            used_nonces.add(evidence['nonce'])
            active = request
        else:
            need(event['approval'] is None, 'UNEXPECTED_APPROVAL')
        if target in {'implementing', 'review', 'done'}:
            need(active is not None and active['context']['revision_id'] == rid, 'APPROVAL_REQUIRED')
        if target in {'awaiting_approval', 'blocked', 'cancelled'}:
            active = None
        state, last_revision = target, rid
    need(state == task['state'] and last_revision == task['current_revision'], 'STATE_MISMATCH')
    for decision in task['decisions']:
        need(decision['revision'] in revisions and decision['selected'] in decision['options'],
             'DECISION_INVALID')
    return active
