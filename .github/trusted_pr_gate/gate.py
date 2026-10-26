"""Read-only gate: executable and policy MUST come from the trusted base SHA.

An Actions job's name/App ID is not an unforgeable required-workflow identity.
See docs/trusted-pr-gate.md before considering any enforcement deployment.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import sys
from urllib.request import Request

REPOSITORY = 'Arch-relic/AgroNaliz'
REPOSITORY_ID = 1411593273
WORKFLOW = '.github/workflows/trusted-pr-gate.yml'
CHECK_NAME = 'trusted-pr-gate'
MAX_JSON = 8 * 1024 * 1024
PROTECTED_ROOTS = ('.github', '.git', 'agent_core', 'tools/agent_mock_b',
                   'tests/agent_core', 'tests/trusted_pr_gate', 'gradle')
PROTECTED_FILES = ('agents.md', 'gradlew', 'gradlew.bat', 'build.gradle', 'build.gradle.kts',
                   'settings.gradle', 'settings.gradle.kts', 'gradle.properties', 'local.properties')
SENSITIVE = ('secret', 'credential', 'permission', 'policy', 'publisher', 'signing',
             'keystore', 'release', 'trusted', 'agent-core', 'agent-identity', 'agent-mock-b')


class Denied(ValueError):
    pass


def need(condition, reason):
    if not condition:
        raise Denied(reason)


def load_json(raw):
    def pairs(items):
        result = {}
        for key, value in items:
            need(key not in result, 'DUPLICATE_JSON_KEY')
            result[key] = value
        return result
    def nonfinite(_):
        raise Denied('NONFINITE_JSON')
    return json.loads(raw, object_pairs_hook=pairs, parse_constant=nonfinite)


def positive(value):
    need(type(value) is int and value > 0, 'INVALID_INTEGER')
    return value


def sha(value):
    need(type(value) is str and re.fullmatch('[0-9a-f]{40}', value) is not None, 'INVALID_SHA')
    return value


def path(value):
    need(type(value) is str and re.fullmatch('[A-Za-z0-9._/-]+', value) is not None, 'INVALID_PATH')
    need(all(p not in ('', '.', '..') for p in value.split('/')), 'INVALID_PATH')
    return value


def protected(value):
    folded = value.lower()
    parts = folded.split('/')
    return (any(folded == p or folded.startswith(p + '/') for p in PROTECTED_ROOTS)
            or folded in PROTECTED_FILES or 'codeowners' in parts
            or any(any(word in part for word in SENSITIVE) or part.endswith(('.pem', '.key', '.jks')) for part in parts))


def policy_document(value):
    need(type(value) is dict and set(value) == {'version', 'repository', 'baseRef', 'maxFiles', 'allowed'}, 'INVALID_POLICY')
    need(type(value['version']) is int and value['version'] == 1
         and value['repository'] == REPOSITORY and value['baseRef'] == 'main', 'INVALID_POLICY')
    need(positive(value['maxFiles']) <= 200, 'INVALID_POLICY')
    need(type(value['allowed']) is list and value['allowed'], 'INVALID_POLICY')
    for rule in value['allowed']:
        need(type(rule) is dict and set(rule) == {'prefix', 'extensions'}, 'INVALID_POLICY')
        need(type(rule['prefix']) is str and rule['prefix'].endswith('/'), 'INVALID_POLICY')
        path(rule['prefix'][:-1])
        need(type(rule['extensions']) is list and rule['extensions'], 'INVALID_POLICY')
        need(all(x in ('.md', '.txt', '.kt', '.java') for x in rule['extensions']), 'INVALID_POLICY')
    return value


def tree(api, ref):
    data = api(f'git/trees/{sha(ref)}?recursive=1')
    need(type(data) is dict and data.get('truncated') is False and type(data.get('tree')) is list, 'INCOMPLETE_TREE')
    files, folded = {}, set()
    for item in data['tree']:
        name = item['path']
        need(type(name) is str and name and name not in files, 'INVALID_TREE')
        if item['type'] == 'tree':
            need(item['mode'] == '040000', 'INVALID_TREE')
            continue
        need(item['type'] in ('blob', 'commit'), 'INVALID_TREE')
        need(name.casefold() not in folded, 'CASE_COLLISION')
        folded.add(name.casefold())
        sha(item['sha'])
        files[name] = (item['sha'], item['mode'], item['type'])
    return files


def verify(api, event, env, policy):
    policy = policy_document(policy)
    need(env['GITHUB_EVENT_NAME'] == 'pull_request_target' and env['GITHUB_REPOSITORY'] == REPOSITORY, 'UNTRUSTED_EVENT')
    need(env['GITHUB_RUN_ATTEMPT'] == '1', 'RERUN_FORBIDDEN')
    need(event['action'] in ('opened', 'synchronize', 'reopened', 'edited', 'ready_for_review'), 'UNTRUSTED_EVENT')
    need(event['repository']['full_name'] == REPOSITORY and event['repository']['id'] == REPOSITORY_ID
         and env['GITHUB_REPOSITORY_ID'] == str(REPOSITORY_ID), 'WRONG_REPOSITORY')
    pr = event['pull_request']; number = positive(pr['number'])
    head, base = sha(pr['head']['sha']), sha(pr['base']['sha'])
    need(pr['base']['ref'] == 'main' and pr['base']['repo']['full_name'] == REPOSITORY, 'WRONG_BASE')
    need(pr['state'] == 'open', 'PR_NOT_OPEN')
    need(env['GITHUB_REF'] == 'refs/heads/main' and env['GITHUB_SHA'] == base, 'WRONG_EXECUTION_BASE')
    need(env['GITHUB_WORKFLOW_REF'] == REPOSITORY + '/' + WORKFLOW + '@refs/heads/main', 'WRONG_WORKFLOW')
    need(env['GITHUB_WORKFLOW_SHA'] == base, 'WRONG_WORKFLOW_SHA')
    run_id = env['GITHUB_RUN_ID']
    need(type(run_id) is str and re.fullmatch('[1-9][0-9]*', run_id), 'INVALID_RUN')
    run = api('actions/runs/' + run_id)
    need(positive(run['id']) == int(run_id) and run['event'] == 'pull_request_target'
         and run['path'] == WORKFLOW and run['run_attempt'] == 1 and run['status'] == 'in_progress'
         and run['repository']['full_name'] == REPOSITORY, 'UNTRUSTED_RUN')
    # GitHub metadata's head_sha is the PR head for target runs; execution SHA is base.
    need(run['head_sha'] == head, 'WRONG_RUN_HEAD')
    def current():
        latest = api(f'pulls/{number}')
        need(latest['number'] == number and latest['state'] == 'open'
             and latest['head']['sha'] == head and latest['base']['sha'] == base
             and latest['base']['ref'] == 'main' and latest['base']['repo']['full_name'] == REPOSITORY
             and latest['head']['repo']['full_name'] == pr['head']['repo']['full_name']
             and latest['changed_files'] == pr['changed_files'], 'PR_CHANGED')
        ref = api('git/ref/heads/main')
        need(ref['object']['sha'] == base and ref['object']['type'] == 'commit', 'BASE_CHANGED')
    current()
    count = positive(pr['changed_files'])
    need(count <= policy['maxFiles'], 'DIFF_TOO_LARGE')
    comparison = api(f'compare/{base}...{head}')
    need(comparison['status'] == 'ahead' and comparison['merge_base_commit']['sha'] == base, 'BASE_NOT_ANCESTOR')
    changes = []
    for page in range(1, 4):
        batch = api(f'pulls/{number}/files?per_page=100&page={page}')
        need(type(batch) is list and len(batch) <= 100, 'INCOMPLETE_DIFF')
        changes.extend(batch)
        need(len(changes) <= count, 'INCOMPLETE_DIFF')
        if len(batch) < 100:
            break
    else:
        raise Denied('INCOMPLETE_DIFF')
    need(len(changes) == count, 'INCOMPLETE_DIFF')
    old, new = tree(api, base), tree(api, head)
    actual = {p for p in old.keys() | new.keys() if old.get(p) != new.get(p)}
    declared, aliases = set(), set()
    for item in changes:
        name = path(item['filename'])
        names = [name]
        if 'previous_filename' in item:
            names.append(path(item['previous_filename']))
        need(not any(protected(n) for n in names), 'PROTECTED_CHANGE')
        need(item['status'] in ('added', 'modified', 'removed'), 'UNSUPPORTED_CHANGE')
        need(len(names) == 1, 'UNSUPPORTED_CHANGE')
        need(name.lower() not in aliases, 'CASE_COLLISION'); aliases.add(name.lower())
        need(name not in declared, 'DUPLICATE_PATH'); declared.add(name)
        need(any(name.startswith(r['prefix']) and any(name.endswith(e) for e in r['extensions'])
                 for r in policy['allowed']), 'PATH_NOT_ALLOWED')
        expected = 'modified' if name in old and name in new else 'added' if name in new else 'removed'
        need(item['status'] == expected and name in actual, 'DIFF_TREE_MISMATCH')
        for snapshot in (old, new):
            if name in snapshot:
                need(snapshot[name][1:] == ('100644', 'blob'), 'NON_REGULAR_FILE')
        need(item['sha'] == (old[name][0] if expected == 'removed' else new[name][0]), 'BLOB_SHA_MISMATCH')
    need(declared == actual, 'DIFF_TREE_MISMATCH')
    current()
    return {'check': CHECK_NAME, 'repository': REPOSITORY, 'pr': number, 'headSha': head,
            'baseSha': base, 'policySha256': hashlib.sha256(json.dumps(policy, sort_keys=True,
                separators=(',', ':')).encode()).hexdigest(), 'files': count,
            'sourceWorkflow': WORKFLOW, 'runId': int(run_id), 'runAttempt': 1}


class API:
    def __init__(self, token):
        need(type(token) is str and bool(token), 'NO_READ_TOKEN')
        self.token = token

    def __call__(self, endpoint):
        need(type(endpoint) is str and not endpoint.startswith('/') and '..' not in endpoint.replace('...', ''), 'INVALID_API_PATH')
        request = Request('https://api.github.com/repos/' + REPOSITORY + '/' + endpoint,
            headers={'Authorization': 'Bearer ' + self.token, 'Accept': 'application/vnd.github+json',
                     'X-GitHub-Api-Version': '2022-11-28'})
        # Refuse all redirects so credentials cannot follow an unexpected host.
        from urllib.request import HTTPRedirectHandler, build_opener
        class NoRedirect(HTTPRedirectHandler):
            def redirect_request(self, req, fp, code, msg, headers, newurl):
                raise Denied('API_REDIRECT')
        with build_opener(NoRedirect()).open(request, timeout=15) as response:
            need(response.status == 200, 'API_FAILURE')
            raw = response.read(MAX_JSON + 1)
        need(len(raw) <= MAX_JSON, 'API_RESPONSE_TOO_LARGE')
        return load_json(raw.decode('utf-8'))


def main():
    try:
        event_path = Path(os.environ['GITHUB_EVENT_PATH'])
        need(event_path.stat().st_size <= MAX_JSON, 'EVENT_TOO_LARGE')
        event = load_json(event_path.read_text())
        policy = load_json(Path(__file__).with_name('policy.json').read_text())
        receipt = verify(API(os.environ['GH_TOKEN']), event, os.environ, policy)
        print(json.dumps(receipt, sort_keys=True))
    except Exception as error:
        # Never print API response bodies, tokens or attacker-controlled messages.
        print('trusted-pr-gate: DENIED (' + type(error).__name__ + ')', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
