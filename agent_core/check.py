"""Read-only CI entrypoints; no execution of candidate code in trusted checks."""
import base64
import json
import os
import re
import sys
from urllib.request import Request, urlopen
from agent_core.core import REPO, OWNER_ID, Denied, require, validate, approval_title, schema, load_json, WORKFLOW


def api(path):
    require(not path.startswith('/') and '..' not in path, 'Invalid API path')
    req = Request(f'https://api.github.com/repos/{REPO}/{path}', headers={
        'Authorization': 'Bearer ' + os.environ['GH_TOKEN'],
        'Accept': 'application/vnd.github+json', 'X-GitHub-Api-Version': '2022-11-28'})
    with urlopen(req, timeout=30) as response:
        return json.load(response)


def document(path, ref):
    data = api(f'contents/{path}?ref={ref}')
    require(data['type'] == 'file' and data['encoding'] == 'base64', 'Expected JSON file')
    return load_json(base64.b64decode(data['content']))


def execution_actor():
    """Identity from GitHub run metadata, within a trusted main runner only.

    Not exposed as a task/dispatch input. Never run this in PR-controlled code.
    Stage 1 does not invoke an autonomous executor.
    """
    require(os.environ['GITHUB_REPOSITORY'] == REPO
            and os.environ['GITHUB_REF'] == 'refs/heads/main'
            and os.environ['GITHUB_WORKFLOW_REF'] == f'{REPO}/{WORKFLOW}@refs/heads/main'
            and os.environ['GITHUB_EVENT_NAME'] == 'workflow_dispatch'
            and os.environ['GITHUB_RUN_ATTEMPT'] == '1', 'Trusted main execution context required')
    run_id = os.environ['GITHUB_RUN_ID']
    require(re.fullmatch('[1-9][0-9]*', run_id), 'Invalid execution run ID')
    run = api(f'actions/runs/{run_id}')
    require(run['id'] == int(run_id) and run['repository']['full_name'] == REPO
            and run['head_repository']['full_name'] == REPO and run['head_branch'] == 'main'
            and run['head_sha'] == os.environ['GITHUB_SHA']
            and re.fullmatch('[0-9a-f]{40}', run['head_sha'])
            and run['event'] == 'workflow_dispatch' and run['run_attempt'] == 1
            and run['status'] == 'in_progress' and run['path'] == WORKFLOW
            and run['workflow_id'] == api('actions/workflows/agent-core.yml')['id'],
            'Untrusted execution run')
    actor = run['actor']['id']
    require(type(actor) is int and actor > 0 and run['triggering_actor']['id'] == actor
            and os.environ['GITHUB_ACTOR_ID'] == str(actor),
            'Ambiguous execution identity')
    return actor


def directory(kind, ref):
    listing = api(f'contents/agent_core/{kind}?ref={ref}')
    require(isinstance(listing, list) and len(listing) < 1000, 'Directory listing incomplete or ambiguous')
    for entry in listing:
        require(entry['type'] == 'file' and entry['path'] == f"agent_core/{kind}/{entry['name']}"
                and (entry['name'] == '.gitkeep' or entry['name'].endswith('.json')),
                'Only flat JSON records are permitted')
    return listing


def trusted_context(event, name):
    require(os.environ['GITHUB_REPOSITORY'] == REPO and os.environ['GITHUB_EVENT_NAME'] == name,
            'Unexpected event source')
    require(os.environ['GITHUB_RUN_ATTEMPT'] == '1', 'Reruns cannot issue authorization')
    if name != 'pull_request_target':
        require(os.environ['GITHUB_REF'] == 'refs/heads/main', 'Trusted operations require main')
        require(event['sender']['id'] == OWNER_ID, 'Owner identity required')


def run(mode, event):
    if mode == 'local':
        from pathlib import Path
        for path in Path('agent_core/tasks').glob('*.json'):
            task = load_json(path.read_bytes())
            require(path.name == task['id'] + '.json', 'Task filename/identity mismatch')
            validate(task, api=api if os.environ.get('GH_TOKEN') else None)
        for path in Path('agent_core/recommendations').glob('*.json'):
            schema(load_json(path.read_bytes()), 'recommendation-v1.json')
        print('Local task/recommendation validation passed (does not establish trust).')
    elif mode == 'pr':
        trusted_context(event, 'pull_request_target')
        pr = event['pull_request']
        def current_pr():
            latest = api(f"pulls/{pr['number']}")
            require(latest['head']['sha'] == pr['head']['sha']
                    and latest['base']['sha'] == pr['base']['sha'], 'PR changed during verification; use a fresh event')
        current_pr()
        require(pr['base']['repo']['full_name'] == REPO and pr['base']['ref'] == 'main', 'Wrong PR base')
        # Read candidate blobs via base-repository commit API; never import candidate modules.
        old_ref, new_ref = pr['base']['sha'], pr['head']['sha']
        for ref in (old_ref, new_ref):
            require(re.fullmatch('[0-9a-f]{40}', ref), 'Invalid commit')
        listing = directory('tasks', old_ref)
        old = {x['name']: document(x['path'], old_ref) for x in listing if x['name'].endswith('.json')}
        listing = directory('tasks', new_ref)
        new = {x['name']: document(x['path'], new_ref) for x in listing if x['name'].endswith('.json')}
        require(old.keys() <= new.keys(), 'Task deletion loses history')
        for name, task in new.items():
            require(name == task['id'] + '.json', 'Task filename/identity mismatch')
            validate(task, old.get(name), api)
        # Governance changes cannot silently substitute the gate's authority.
        files = []
        for page in range(1, 101):
            batch = api(f"pulls/{pr['number']}/files?per_page=100&page={page}")
            files.extend(batch)
            if len(batch) < 100:
                break
        require(len(files) == pr['changed_files'], 'Incomplete PR diff')
        changed_paths = {x['filename'] for x in files}
        changed_paths.update(x['previous_filename'] for x in files if 'previous_filename' in x)
        protected = [path for path in changed_paths if
                     path.startswith(('agent_core/', '.github/workflows/'))
                     and not path.startswith(('agent_core/tasks/', 'agent_core/recommendations/'))]
        require(not protected, 'Governance/workflow changes require separate owner review; gate cannot authorize itself')
        listing = directory('recommendations', new_ref)
        for entry in listing:
            if entry['name'].endswith('.json'):
                schema(document(entry['path'], new_ref), 'recommendation-v1.json')
        current_pr()
    elif mode == 'approve':
        trusted_context(event, 'workflow_dispatch')
        inputs = event['inputs']
        require(inputs['mode'] == 'approve', 'Wrong dispatch mode')
        require(re.fullmatch('AGN-[0-9]+', inputs['task']), 'Invalid task ID')
        task = document('agent_core/tasks/' + inputs['task'] + '.json', os.environ['GITHUB_SHA'])
        validate(task, api=api)
        require(task['executor_id'] != OWNER_ID and task['state'] == 'awaiting_approval', 'Not approvable/self-approval')
        revision = next(r for r in task['revisions'] if r['spec']['id'] == task['current_revision'])
        from agent_core.core import PROTECTED, digest
        require(not PROTECTED.intersection(revision['spec']['operations']), 'Protected operations unsupported')
        expected = approval_title(task, revision, len(task['history']))
        actual = f"approve:{inputs['task']}:{inputs['spec_hash']}:{inputs['operations_hash']}:{inputs['executor_id']}:{inputs['sequence']}"
        require(actual == expected, 'Approval inputs disagree with immutable main specification')
        print(expected)
    elif mode == 'analysis':
        name = os.environ['GITHUB_EVENT_NAME']
        require(name in {'schedule', 'workflow_dispatch'}, 'Invalid analysis event')
        if name == 'schedule':
            require(os.environ['GITHUB_REPOSITORY'] == REPO and os.environ['GITHUB_REF'] == 'refs/heads/main'
                    and event['schedule'] == '17 6 * * 1', 'Invalid weekly schedule')
        else:
            trusted_context(event, name)
            require(event['inputs']['mode'] == 'session_start', 'Wrong analysis mode')
        schema({'schema_version':1,'id':'contract-check','trigger':'weekly' if name == 'schedule' else 'session_start',
                'created_at':'2026-10-20T00:00:00Z','title':'Contract fixture','rationale':'Schema check only',
                'priority':'low','evidence':['docs/agent-core.md'],'proposed_operations':['test'],'status':'proposed'},
               'recommendation-v1.json')
        print('Analysis contract valid; AI generation and notifications are not integrated.')
    else:
        raise Denied('Unknown mode')


if __name__ == '__main__':
    try:
        mode = sys.argv[1]
        run(mode, {} if mode == 'local' else load_json(open(os.environ['GITHUB_EVENT_PATH']).read()))
    except Exception as exc:
        print(f'DENIED: {type(exc).__name__}: {exc}', file=sys.stderr)
        sys.exit(1)
