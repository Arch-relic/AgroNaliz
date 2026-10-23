import copy
import unittest
from unittest.mock import patch

from tools.agent_mock_b.canonical import Refusal, sha
from tools.agent_mock_b.diff_validator import apply
from tools.agent_mock_b.snapshot import Snapshot
from test_acceptance_15 import Fixture, modify


class SnapshotDiff(unittest.TestCase):
    def test_add_modify_delete_and_immutable_original(self):
        f = Fixture(self)
        for op in ([modify(op='ADD', path='scratch/new')], [modify()], [modify(op='DELETE')]):
            result = apply(f.spec, f.snapshot, op)
            result.verify()
            self.assertEqual(dict(f.snapshot.files), {'scratch/a.txt': b'a\n'})
        with self.assertRaises(TypeError):
            f.snapshot.files['scratch/a.txt'] = b'changed'

    def test_ascii_posix_and_case_collisions(self):
        for name in ('/a', 'a/', 'a//b', '.', '..', 'a/../b', 'a/./b', 'C:/a', 'a\\b', 'é', 'a b'):
            with self.subTest(name=name), self.assertRaises(Refusal):
                Snapshot({name: b'x'})
        for files in ({'a': b'x', 'A': b'y'}, {'a': b'x', 'a/b': b'y'}):
            with self.assertRaises(Refusal):
                Snapshot(files)
        snapshot = Snapshot({'a': b'\r\n'}); snapshot.verify()
        self.assertEqual(snapshot.manifest()[0]['mode'], '100644')
        self.assertEqual(snapshot.manifest()[0]['contentType'], 'text/plain; charset=utf-8')

    def test_utf8_bom_nul_invalid_and_bytes_hash(self):
        for raw in (b'\xef\xbb\xbftext', b'a\0', b'\xff', b'\xed\xa0\x80'):
            with self.subTest(raw=raw), self.assertRaises(Refusal):
                Snapshot({'x': raw})
        self.assertNotEqual(Snapshot({'x': b'a\r\n'}).digest, Snapshot({'x': b'a\n'}).digest)

    def test_line_count_exact_examples(self):
        for old, new, count in [(b'a\n', b'a\nb\n', 1), (b'a\n', b'b\n', 2),
                                (b'a\r\n', b'a\n', 0), (b'a\r', b'a\n', 0), (b'a', b'a\n', 2)]:
            with self.subTest(old=old, new=new):
                f = Fixture(self, files={'scratch/a.txt': old}, limits={'maxDiffLines': max(1, count)})
                apply(f.spec, f.snapshot, [modify(old=old, raw=new)])
                if count > 1:
                    f.spec['limits']['maxDiffLines'] = count - 1
                    with self.assertRaisesRegex(Refusal, 'LIMIT_EXCEEDED'):
                        apply(f.spec, f.snapshot, [modify(old=old, raw=new)])

    def test_canonical_base64_padding_and_hashes(self):
        f = Fixture(self)
        for value in ('Yg=', 'Yg===', 'Yh==', 'Yg==\n', '____', 'Y g=='):
            op = modify(raw=b'b'); op['newContentBase64'] = value
            with self.subTest(value=value), self.assertRaises(Refusal):
                apply(f.spec, f.snapshot, [op])
        for key in ('oldSha256', 'newSha256'):
            op = modify(); op[key] = '0' * 64
            with self.assertRaisesRegex(Refusal, 'HASH_MISMATCH'):
                apply(f.spec, f.snapshot, [op])

    def test_resource_bounds_before_sequence_matcher(self):
        f = Fixture(self)
        with patch('tools.agent_mock_b.diff_validator.SequenceMatcher', side_effect=AssertionError('too early')):
            for raw in (b'x' * 2097153, b'x\n' * 20001):
                with self.subTest(length=len(raw)), self.assertRaisesRegex(Refusal, 'LIMIT_EXCEEDED'):
                    apply(f.spec, f.snapshot, [modify(raw=raw)])
        # Exactly 2 MiB combined changed bytes, and exactly 20,000 lines, are allowed.
        raw = b'x' * (2097152 - 2)
        f.spec['limits']['maxDiffLines'] = 40000
        apply(f.spec, f.snapshot, [modify(raw=raw)])
        apply(f.spec, f.snapshot, [modify(raw=b'x\n' * 20000)])

    def test_protection_cannot_be_weakened_by_allowlist(self):
        f = Fixture(self); f.spec['allowedPaths'] = ['dir:docs/', 'dir:.git/', 'dir:.github/', 'dir:app/', 'dir:keys/']
        for name in ('docs/codex-handoff.md', 'docs/agent-core.md', '.git/config', '.github/x',
                     'app/x', 'keys/test.pem', 'docs/identity.txt', 'docs/policy.txt'):
            with self.subTest(name=name), self.assertRaisesRegex(Refusal, 'PROTECTED_PATH'):
                apply(f.spec, f.snapshot, [modify(path=name, op='ADD')])

    def test_file_allowlist_segment_boundary_and_op_contract(self):
        f = Fixture(self); f.spec['allowedPaths'] = ['dir:scratch/sub/']
        with self.assertRaisesRegex(Refusal, 'INVALID_PATH'):
            apply(f.spec, f.snapshot, [modify(path='scratch/submarine/a', op='ADD')])
        f.spec['allowedPaths'] = ['file:scratch/a.txt']
        apply(f.spec, f.snapshot, [modify()])
        f.spec['allowedOperations'] = ['ADD']
        with self.assertRaisesRegex(Refusal, 'INVALID_DIFF'):
            apply(f.spec, f.snapshot, [modify()])
        for op in (modify(path='scratch/a.txt', op='ADD'), modify(path='scratch/missing', op='DELETE')):
            f.spec['allowedOperations'] = ['ADD', 'DELETE', 'MODIFY']; f.spec['allowedPaths'] = ['dir:scratch/']
            with self.assertRaisesRegex(Refusal, 'INVALID_DIFF'):
                apply(f.spec, f.snapshot, [op])
