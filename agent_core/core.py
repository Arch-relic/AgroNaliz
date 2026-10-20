"""Fail-closed task validation. API responses must come from GitHub, never PR files."""
from datetime import datetime
import hashlib
import json
from pathlib import Path
from jsonschema import Draft202012Validator, FormatChecker

REPO = 'Arch-relic/AgroNaliz'
OWNER_ID = 240867647
WORKFLOW = '.github/workflows/agent-core.yml'
PROTECTED = {'room', 'backup', 'signing', 'production_policy', 'merge_main', 'publish_apk'}
EDGES = {
    'proposed': {'awaiting_approval', 'cancelled', 'blocked'},
    'awaiting_approval': {'approved', 'cancelled', 'blocked'},
    'approved': {'implementing', 'awaiting_approval', 'cancelled', 'blocked'},
    'implementing': {'review', 'awaiting_approval', 'cancelled', 'blocked'},
    'review': {'done', 'implementing', 'awaiting_approval', 'cancelled', 'blocked'},
    'blocked': {'awaiting_approval', 'cancelled'}, 'cancelled': set(), 'done': set(),
}

class Denied(ValueError):
    pass


def require(condition, reason):
    if not condition:
        raise Denied(reason)


def load_json(raw):
    def unique(pairs):
        result = {}
        for key, value in pairs:
            require(key not in result, 'Duplicate JSON key: ' + key)
            result[key] = value
        return result
    def invalid(value):
        raise Denied('Non-finite JSON value: ' + value)
    return json.loads(raw, object_pairs_hook=unique, parse_constant=invalid)


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(',', ':'),
                                     ensure_ascii=True, allow_nan=False).encode()).hexdigest()


def schema(value, name):
    definition = json.loads((Path(__file__).parent / 'schemas' / name).read_text())
    errors = sorted(Draft202012Validator(definition, format_checker=FormatChecker()).iter_errors(value),
                    key=lambda e: str(e.path))
    require(not errors, 'Schema: ' + '; '.join(e.message for e in errors))


def approval_title(task, revision, sequence):
    return f"approve:{task['id']}:{revision['hash']}:{digest(revision['spec']['operations'])}:{task['executor_id']}:{sequence}"


def verify_approval(task, revision, sequence, run_id, api):
    require(api is not None, 'GitHub verification required; local evidence is not authority')
    run = api(f'actions/runs/{run_id}')
    require(run['id'] == run_id and run['repository']['full_name'] == REPO
            and run['head_repository']['full_name'] == REPO, 'Wrong repository/run')
    require(run['actor']['id'] == OWNER_ID and run['triggering_actor']['id'] == OWNER_ID,
            'Only the verified GitHub owner may approve')
    require(task['executor_id'] != OWNER_ID, 'Executor cannot self-approve')
    require(run['event'] == 'workflow_dispatch' and run['head_branch'] == 'main'
            and run['run_attempt'] == 1 and run['status'] == 'completed'
            and run['conclusion'] == 'success' and run['path'] == WORKFLOW, 'Untrusted approval event')
    workflow = api('actions/workflows/agent-core.yml')
    require(run['workflow_id'] == workflow['id'], 'Wrong workflow identity')
    require(run['display_title'] == approval_title(task, revision, sequence), 'Approval scope/replay mismatch')
    # Independently read the immutable main commit that issued the approval.
    trusted = api(f"contents/agent_core/tasks/{task['id']}.json?ref={run['head_sha']}")
    import base64
    original = load_json(base64.b64decode(trusted['content'], validate=True))
    require(original['id'] == task['id'] and original['executor_id'] == task['executor_id']
            and original['current_revision'] == revision['spec']['id']
            and original['revisions'] == task['revisions'][:task['revisions'].index(revision)+1]
            and original['state'] == 'awaiting_approval'
            and original['history'] == task['history'][:sequence]
            and original['issue'] == task['issue'] and original['type'] == task['type'], 'Approval not bound to immutable task revision/sequence')
    require(not PROTECTED.intersection(revision['spec']['operations']),
            'Protected operations need a separate authorization protocol; unsupported in stage 1')


def validate(task, previous=None, api=None):
    schema(task, 'task-v1.json')
    revisions = {r['spec']['id']: r for r in task['revisions']}
    require(len(revisions) == len(task['revisions']), 'Duplicate revision ID')
    for r in revisions.values():
        require(r['hash'] == digest(r['spec']), 'Specification hash mismatch')
    require(task['current_revision'] in revisions, 'Missing current specification')
    if previous:
        for field in ('id', 'issue', 'executor_id', 'type'):
            require(task[field] == previous[field], 'Immutable task identity changed')
        for field in ('revisions', 'history', 'decisions'):
            require(task[field][:len(previous[field])] == previous[field], f'{field} must be append-only')
    state, active, last_revision = None, None, None
    last_time = None
    used = set()
    for i, event in enumerate(task['history']):
        target, rid = event['to'], event['revision']
        instant = datetime.fromisoformat(event['at'].replace('Z', '+00:00'))
        require(last_time is None or instant >= last_time, 'History timestamps must be ordered')
        last_time = instant
        require(rid in revisions, 'History references missing revision')
        require(event['from'] == state, 'Contradictory transition source')
        if state is None:
            require(target == 'proposed' and i == 0, 'Task must register as proposed')
        else:
            require(target in EDGES[state], f'Forbidden transition {state} -> {target}')
        if rid != last_revision:
            require(target in {'proposed', 'awaiting_approval'}, 'Revision change requires approval again')
            active = None
        if target == 'approved':
            run = event['approval_run']
            require(run is not None and run not in used, 'Missing or reused approval')
            verify_approval(task, revisions[rid], i, run, api)
            used.add(run)
            active = rid
        else:
            require(event['approval_run'] is None, 'Unexpected approval evidence')
        if target in {'implementing', 'review', 'done'}:
            require(active == rid, 'Development requires approved specification')
        if target in {'awaiting_approval', 'blocked', 'cancelled'}:
            active = None
        state, last_revision = target, rid
    require(state == task['state'] and last_revision == task['current_revision'], 'State/revision disagrees with history')
    for decision in task['decisions']:
        require(decision['revision'] in revisions and decision['selected'] in decision['options'], 'Invalid decision history')
    return task


def authorize(task, operation, api=None, *, actor_id=None):
    require(actor_id == task['executor_id'], 'Verified executor identity required')
    validate(task, api=api)
    require(operation not in PROTECTED, 'Separate protected-operation permission required')
    require(task['state'] in {'approved', 'implementing', 'review'}, 'Task is not authorized for work')
    current = next(r for r in task['revisions'] if r['spec']['id'] == task['current_revision'])
    require(operation in current['spec']['operations'], 'Operation outside approved scope')
