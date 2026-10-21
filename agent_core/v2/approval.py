"""Owner evidence bound to one immutable v2 request and history position."""
from agent_core.v2.model import app_task, instant, need
from agent_core.v2.trust import verify_run


def verify_approval(task, request, event, position, transport, registry, anchors):
    revision = next(r for r in task['revisions'] if r['spec']['id'] == request['context']['revision_id'])
    need(revision['spec']['operations'] == ['test'], 'OPERATION_FORBIDDEN')
    run_id = event['approval']['run_id']
    run = transport.read(f'run/{run_id}')
    need(type(run['id']) is int and run['id'] == run_id, 'RUN_ID_MISMATCH')
    verify_run(run, registry['approval_workflow'], anchors, approval=True)
    need(run['display_title'] == f"approve-v2:{task['id']}:{request['context_hash']}",
         'APPROVAL_TITLE_MISMATCH')
    original = transport.read(f"task/{task['id']}@{run['head_sha']}")
    app_task(original)
    for key in ('id', 'issue', 'type', 'executor', 'predecessor_task_id'):
        need(original.get(key) == task.get(key), 'APPROVAL_TASK_MISMATCH')
    ctx = request['context']
    revision_index = next(i for i, r in enumerate(task['revisions']) if r['spec']['id'] == ctx['revision_id'])
    request_index = task['approval_requests'].index(request)
    need(original['state'] == 'awaiting_approval' and original['current_revision'] == ctx['revision_id']
         and original['history'] == task['history'][:position]
         and original['revisions'] == task['revisions'][:revision_index + 1]
         and original['approval_requests'] == task['approval_requests'][:request_index + 1],
         'IMMUTABLE_REQUEST_MISMATCH')
    need(ctx['policy_hash'] == registry['policy_hash'], 'APPROVAL_POLICY_MISMATCH')
    created, completed = instant(run['created_at']), instant(run['updated_at'])
    need(instant(ctx['not_before']) <= created <= completed < instant(ctx['not_after'])
         and completed <= instant(event['at']), 'APPROVAL_TIME_INVALID')
