"""Synthetic API tests: no network, credentials, PR execution or repository writes."""
import ast
import copy
import importlib.util
import json
from pathlib import Path
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('trusted_gate', ROOT / '.github/trusted_pr_gate/gate.py')
gate = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(gate)


class Fixture:
    def __init__(self):
        self.policy = json.loads((ROOT / '.github/trusted_pr_gate/policy.json').read_text())
        self.base, self.head = 'a' * 40, 'b' * 40
        self.pr = {'number': 1, 'state': 'open', 'changed_files': 1,
                   'base': {'sha': self.base, 'ref': 'main', 'repo': {'full_name': gate.REPOSITORY}},
                   'head': {'sha': self.head, 'repo': {'full_name': 'synthetic/fork'}}}
        self.event = {'action': 'opened', 'repository': {'full_name': gate.REPOSITORY, 'id': gate.REPOSITORY_ID}, 'pull_request': copy.deepcopy(self.pr)}
        self.env = {'GITHUB_EVENT_NAME': 'pull_request_target', 'GITHUB_REPOSITORY': gate.REPOSITORY,
                    'GITHUB_REPOSITORY_ID': str(gate.REPOSITORY_ID),
                    'GITHUB_REF': 'refs/heads/main', 'GITHUB_SHA': self.base, 'GITHUB_WORKFLOW_SHA': self.base,
                    'GITHUB_WORKFLOW_REF': gate.REPOSITORY + '/' + gate.WORKFLOW + '@refs/heads/main',
                    'GITHUB_RUN_ATTEMPT': '1', 'GITHUB_RUN_ID': '12'}
        self.run = {'id': 12, 'event': 'pull_request_target', 'path': gate.WORKFLOW, 'run_attempt': 1,
                    'status': 'in_progress', 'repository': {'full_name': gate.REPOSITORY}, 'head_sha': self.head}
        self.files = [{'filename': 'docs/notes.md', 'status': 'modified', 'sha': 'd' * 40}]
        self.old = {'docs/notes.md': ('c' * 40, '100644', 'blob')}
        self.new = {'docs/notes.md': ('d' * 40, '100644', 'blob')}
        self.calls = []
        self.override = lambda endpoint, value: value

    def api(self, endpoint):
        self.calls.append(endpoint)
        if endpoint == 'actions/runs/12':
            value = self.run
        elif endpoint == 'pulls/1':
            value = self.pr
        elif endpoint == 'git/ref/heads/main':
            value = {'object': {'sha': self.base, 'type': 'commit'}}
        elif endpoint.startswith('compare/'):
            value = {'status': 'ahead', 'merge_base_commit': {'sha': self.base}}
        elif endpoint.startswith('pulls/1/files?'):
            page = int(endpoint.rsplit('=', 1)[1])
            value = self.files[(page - 1) * 100:page * 100]
        elif endpoint.startswith('git/trees/'):
            files = self.old if endpoint.split('/')[2].startswith(self.base) else self.new
            value = {'truncated': False, 'tree': [dict(path=p, sha=v[0], mode=v[1], type=v[2]) for p, v in files.items()]}
        else:
            raise AssertionError('Unexpected API endpoint: ' + endpoint)
        return self.override(endpoint, copy.deepcopy(value))

    def verify(self):
        return gate.verify(self.api, self.event, self.env, self.policy)

    def rename_path(self, name):
        self.old = {name: ('c' * 40, '100644', 'blob')}
        self.new = {name: ('d' * 40, '100644', 'blob')}
        self.files[0]['filename'] = name


class RequiredScenarios(unittest.TestCase):
    def test_01_safe_pr(self):
        f = Fixture(); receipt = f.verify()
        self.assertEqual(receipt['headSha'], f.head)
        self.assertEqual(receipt['baseSha'], f.base)
        self.assertEqual(receipt['check'], 'trusted-pr-gate')
        self.assertEqual(f.calls.count('pulls/1'), 2)

    def test_02_workflow_change_and_rename_source(self):
        for name in ('.github/workflows/a.yml', '.GITHUB/workflows/a.yml'):
            f = Fixture(); f.rename_path(name)
            with self.subTest(name=name), self.assertRaisesRegex(gate.Denied, 'PROTECTED_CHANGE'):
                f.verify()
        f = Fixture(); f.files[0].update(status='renamed', previous_filename='.github/workflows/old.yml')
        with self.assertRaisesRegex(gate.Denied, 'PROTECTED_CHANGE'):
            f.verify()

    def test_03_checker_and_publisher_changes(self):
        for name in ('.github/trusted_pr_gate/gate.py', '.github/trusted_pr_gate/policy.json',
                     'tools/agent_mock_b/simulated_publisher.py', 'docs/publisher-settings.md',
                     'docs/agent-mock-b/protocol-v1.md'):
            f = Fixture(); f.rename_path(name)
            with self.subTest(name=name), self.assertRaisesRegex(gate.Denied, 'PROTECTED_CHANGE'):
                f.verify()

    def test_04_agent_permissions_and_signing(self):
        for name in ('agent_core/registry.json', 'agent_core/v2/approval.py', 'docs/access-policy.md',
                     'keys/production.jks', 'docs/release-signing.md'):
            f = Fixture(); f.rename_path(name)
            with self.subTest(name=name), self.assertRaisesRegex(gate.Denied, 'PROTECTED_CHANGE'):
                f.verify()

    def test_05_all_codeowners_locations_and_case(self):
        for name in ('CODEOWNERS', '.github/CODEOWNERS', 'docs/CODEOWNERS', 'docs/codeowners', 'other/CODEOWNERS'):
            f = Fixture(); f.rename_path(name)
            with self.subTest(name=name), self.assertRaisesRegex(gate.Denied, 'PROTECTED_CHANGE'):
                f.verify()

    def test_06_head_and_base_change_during_check(self):
        for field in ('head', 'base'):
            f = Fixture()
            def change(endpoint, value):
                if endpoint == 'pulls/1' and f.calls.count(endpoint) == 2:
                    value[field]['sha'] = 'e' * 40
                return value
            f.override = change
            with self.subTest(field=field), self.assertRaisesRegex(gate.Denied, 'PR_CHANGED'):
                f.verify()
        f = Fixture()
        f.override = lambda endpoint, value: {'object': {'sha': 'f' * 40, 'type': 'commit'}} if endpoint == 'git/ref/heads/main' else value
        with self.assertRaisesRegex(gate.Denied, 'BASE_CHANGED'):
            f.verify()

    def test_07_api_unavailable_at_every_read(self):
        reference = Fixture(); reference.verify()
        for index in range(len(reference.calls)):
            f = Fixture()
            def unavailable(endpoint, value):
                if len(f.calls) - 1 == index:
                    raise OSError('synthetic API unavailable')
                return value
            f.override = unavailable
            with self.subTest(read=index), self.assertRaises(OSError):
                f.verify()

    def test_08_unknown_incomplete_and_omitted_changes(self):
        for endpoint in ('actions/runs/12', 'pulls/1', 'git/ref/heads/main'):
            f = Fixture(); f.override = lambda e, v: {} if e == endpoint else v
            with self.subTest(endpoint=endpoint), self.assertRaises((KeyError, gate.Denied)):
                f.verify()
        f = Fixture(); f.files.clear()
        with self.assertRaisesRegex(gate.Denied, 'INCOMPLETE_DIFF'):
            f.verify()
        f = Fixture(); f.new['docs/hidden.md'] = ('e' * 40, '100644', 'blob')
        with self.assertRaisesRegex(gate.Denied, 'DIFF_TREE_MISMATCH'):
            f.verify()
        f = Fixture(); f.files[0]['status'] = 'unknown'
        with self.assertRaisesRegex(gate.Denied, 'UNSUPPORTED_CHANGE'):
            f.verify()

    def test_09_no_candidate_code_or_dependencies_executed(self):
        f = Fixture()
        # Only repository metadata and tree identities are read, never blob contents.
        with patch('builtins.eval', side_effect=AssertionError('candidate execution')), \
             patch('builtins.exec', side_effect=AssertionError('candidate execution')):
            f.verify()
        self.assertFalse(any(e.startswith(('contents/', 'git/blobs/')) for e in f.calls))
        workflow = (ROOT / '.github/workflows/trusted-pr-gate.yml').read_text()
        self.assertIn('ref: ${{ github.event.pull_request.base.sha }}', workflow)
        self.assertNotIn('head.sha', workflow)
        self.assertNotIn('pip install', workflow)
        self.assertNotIn('secrets.', workflow)
        self.assertNotIn('write', workflow)
        self.assertNotIn('if:', workflow)
        tree = ast.parse((ROOT / '.github/trusted_pr_gate/gate.py').read_text())
        self.assertFalse(any(isinstance(n, ast.Import) and any(a.name in ('subprocess', 'importlib') for a in n.names) for n in ast.walk(tree)))

    def test_10_rerun_has_no_stale_authorization(self):
        f = Fixture(); f.verify()
        f.env['GITHUB_RUN_ATTEMPT'] = '2'
        with self.assertRaisesRegex(gate.Denied, 'RERUN_FORBIDDEN'):
            f.verify()
        f = Fixture(); f.run['run_attempt'] = 2
        with self.assertRaisesRegex(gate.Denied, 'UNTRUSTED_RUN'):
            f.verify()


class AdditionalSecurity(unittest.TestCase):
    def test_pagination_boundary_and_complete_tree(self):
        for count in (100, 101, 200):
            f = Fixture(); f.pr['changed_files'] = f.event['pull_request']['changed_files'] = count
            f.old = {}; f.new = {}; f.files = []
            for i in range(count):
                name = f'docs/n{i}.md'
                f.new[name] = ('d' * 40, '100644', 'blob')
                f.files.append(dict(filename=name, status='added', sha='d' * 40))
            self.assertEqual(f.verify()['files'], count)
            self.assertIn('page=' + str(count // 100 + 1), '\n'.join(f.calls))
        f = Fixture()
        def truncated(endpoint, value):
            if endpoint.startswith('git/trees/'):
                value['truncated'] = True
            return value
        f.override = truncated
        with self.assertRaisesRegex(gate.Denied, 'INCOMPLETE_TREE'):
            f.verify()

    def test_duplicate_case_alias_and_modes(self):
        f = Fixture(); f.new['DOCS/NOTES.MD'] = ('d' * 40, '100644', 'blob')
        with self.assertRaisesRegex(gate.Denied, 'CASE_COLLISION'):
            f.verify()
        for mode, kind in (('120000', 'blob'), ('100755', 'blob'), ('160000', 'commit')):
            f = Fixture(); f.new['docs/notes.md'] = ('d' * 40, mode, kind)
            with self.subTest(mode=mode), self.assertRaisesRegex(gate.Denied, 'NON_REGULAR_FILE'):
                f.verify()

    def test_json_policy_and_workflow_identity(self):
        with self.assertRaises(gate.Denied):
            gate.load_json('{"a":1,"a":2}')
        with self.assertRaises(gate.Denied):
            gate.load_json('{"a":NaN}')
        for key in ('GITHUB_SHA', 'GITHUB_WORKFLOW_SHA', 'GITHUB_WORKFLOW_REF', 'GITHUB_EVENT_NAME'):
            f = Fixture(); f.env[key] = 'untrusted'
            with self.subTest(key=key), self.assertRaises(gate.Denied):
                f.verify()
        f = Fixture(); f.policy['unknown'] = True
        with self.assertRaises(gate.Denied):
            f.verify()

    def test_scope_and_stale_base(self):
        for name in ('docs/../safe.md', '/docs/safe.md', 'docs//safe.md', 'docs/é.md', 'unapproved/safe.md'):
            f = Fixture(); f.rename_path(name)
            with self.subTest(name=name), self.assertRaises(gate.Denied):
                f.verify()
        f = Fixture()
        f.override = lambda e, v: {'status': 'diverged', 'merge_base_commit': {'sha': 'f' * 40}} if e.startswith('compare/') else v
        with self.assertRaisesRegex(gate.Denied, 'BASE_NOT_ANCESTOR'):
            f.verify()

    def test_unknown_exception_exits_nonzero_without_secret_output(self):
        with patch.dict('os.environ', {}, clear=True), patch('sys.stderr') as stderr:
            self.assertEqual(gate.main(), 1)
        output = ''.join(str(x) for call in stderr.write.call_args_list for x in call.args)
        self.assertIn('DENIED', output)
        self.assertNotIn('GH_TOKEN', output)

    def test_controlled_name_and_actions_app_collision_is_not_a_trust_proof(self):
        # A counterexample to name+App-ID enforcement; NOT a fake live proof.
        trusted = {'name': 'trusted-pr-gate', 'app': {'id': 15368}, 'conclusion': 'success',
                   'workflow': '.github/workflows/trusted-pr-gate.yml', 'event': 'pull_request_target'}
        forged = dict(trusted, workflow='.github/workflows/attacker.yml', event='pull_request')
        selector = lambda c: (c['name'], c['app']['id'])
        self.assertEqual(selector(trusted), selector(forged))
        self.assertNotEqual(trusted['workflow'], forged['workflow'])
        # Model the documented native required-check acceptance, NOT a safe gate.
        native_accepts = lambda c: c is not None and selector(c) == selector(trusted) and c['conclusion'] in ('success', 'skipped', 'neutral')
        self.assertTrue(native_accepts(forged))
        for conclusion in ('skipped', 'neutral'):
            self.assertTrue(native_accepts(dict(forged, conclusion=conclusion)))
        self.assertFalse(native_accepts(None))
        self.assertFalse(native_accepts(dict(forged, conclusion='failure')))
        # Therefore no claim of non-spoofability is made from name+App-ID tests.
