"""All fifteen acceptance groups, using entirely synthetic data and clocks."""
import base64
import copy
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from pathlib import Path
import tempfile
import unittest
from uuid import UUID

from tools.agent_mock_b import PROTOCOL
from tools.agent_mock_b.approval_store import ApprovalStore, Trust
from tools.agent_mock_b.canonical import Refusal, digest, dumps, sha
from tools.agent_mock_b.contracts import DENY, LIMITS
from tools.agent_mock_b.controller import Controller
from tools.agent_mock_b.evidence import Evidence
from tools.agent_mock_b.fake_worker import FakeWorker
from tools.agent_mock_b.recovery import replay, recovery_decision
from tools.agent_mock_b.snapshot import Snapshot


def uid(number):
    return str(UUID(int=number))


class Crash(RuntimeError):
    pass


@dataclass
class Clock:
    wall: str = '2026-10-23T10:00:00.000Z'
    elapsed: int = 0

    def now(self):
        return self.wall

    def monotonic_ms(self):
        return self.elapsed


def modify(path='scratch/a.txt', raw=b'b\n', old=b'a\n', op='MODIFY'):
    return dict(op=op, path=path, oldSha256=None if op == 'ADD' else sha(old),
                newSha256=None if op == 'DELETE' else sha(raw),
                newContentBase64=None if op == 'DELETE' else base64.b64encode(raw).decode(),
                newMode=None if op == 'DELETE' else '100644')


class Fixture:
    """Fixture metadata lists every required observable; assertions use fresh replay."""
    def __init__(self, testcase, limits=None, files=None):
        self.directory = tempfile.TemporaryDirectory(prefix='mock-b-test-')
        testcase.addCleanup(self.directory.cleanup)
        self.clock = Clock()
        self.failpoint = None
        self.snapshotBytes = dict(files or {'scratch/a.txt': b'a\n'})
        self.snapshot = Snapshot(self.snapshotBytes)
        self.spec = dict(protocolVersion=PROTOCOL, repository='Arch-relic/AgroNaliz',
            taskId=uid(1), revision=1, baseSha='a' * 40, snapshotDigest=self.snapshot.digest,
            allowedPaths=['dir:scratch/'], protectedPaths=sorted('dir:' + x if x.endswith('/') else 'file:' + x for x in DENY),
            allowedOperations=['ADD', 'DELETE', 'MODIFY'], acceptanceCriteria=['Synthetic bytes match'],
            limits=dict(maxRunsPerTask=3, maxOperationsPerRun=3, maxDiffFiles=3, maxDiffLines=8,
                        maxOutputBytes=4096, maxWallDurationMs=1000, maxTotalOutputBytesPerTask=4096),
            workerKind='mock', publishAllowed=False)
        self.spec['limits'].update(limits or {})
        self.approvalStore = ApprovalStore()
        self.record = dict(approvalId=uid(2), taskId=uid(1), revision=1, taskDigest=digest('spec', self.spec),
            ownerId='synthetic-owner', issuedAt='2026-10-23T09:00:00.000Z', expiresAt='2026-10-23T11:00:00.000Z',
            scope=['dir:scratch/'], status='ACTIVE', storeVersion=1)
        self.approvalStore.put(self.record)
        self.envelope = {k: self.record[k] for k in ('approvalId', 'taskDigest', 'ownerId')}
        self.repositoryStore = __import__('tools.agent_mock_b.repository_store', fromlist=['RepositoryStore']).RepositoryStore(
            dict(repository=self.spec['repository'], baseSha=self.spec['baseSha'], snapshotDigest=self.snapshot.digest, storeVersion=1), self.snapshot)
        self.evidence = Evidence(self.directory.name, uid(1), uid(3))
        self.diff = [modify()]
        self.workerResponse = None
        self.worker = FakeWorker(self.response)
        self.controller = Controller(Trust('synthetic-owner'), self.approvalStore, self.repositoryStore,
                                     self.evidence, self.worker, self.clock, self.hook)
        self.expectedTaskState = 'REVIEWABLE'
        self.expectedRunState = 'READY_FOR_OWNER_REVIEW'
        self.expectedEvents = ['APPROVAL_CHECKED', 'INTENT', 'RESULT', 'INTENT', 'RESULT', 'INTENT',
                               'SIMULATED_PUBLISH_REQUESTED', 'SIMULATED_PUBLISHED', 'RESULT']
        self.workerStarts, self.publisherCalls, self.externalCalls = 1, 1, 0
        self.confirmedBudget = {'runs': 1, 'bytes': min(self.spec['limits']['maxOutputBytes'], self.spec['limits']['maxTotalOutputBytesPerTask'])}
        self.checkpointRoot = None  # Compared to the independently read actual checkpoint.
        self.run_id = uid(10)
        self.ops = {'WORKER': uid(11), 'VALIDATOR': uid(12), 'SIMULATED_PUBLISH': uid(13)}

    def response(self, request):
        value = dict(request, diffOps=copy.deepcopy(self.diff), report='synthetic success')
        return self.workerResponse if self.workerResponse is not None else dumps(value)

    def hook(self, name):
        if self.failpoint is not None:
            self.failpoint(name)

    def run(self, **kw):
        return self.controller.execute(self.spec, self.envelope, self.run_id, self.ops, **kw)

    def revoke(self):
        record = dict(self.record, status='REVOKED', storeVersion=2)
        self.approvalStore.put(record)


class Acceptance15(unittest.TestCase):
    def refused(self, fixture, code=None, worker=0, publisher=0):
        with self.assertRaises(Refusal) as error:
            fixture.run()
        if code:
            self.assertEqual(error.exception.code, code)
        self.assertEqual(fixture.worker.workerStarts, worker)
        self.assertEqual(fixture.controller.publisherCalls, publisher)
        self.assertEqual(fixture.controller.externalCalls, 0)
        return error.exception.code

    def test_01_missing_approval_and_protection_without_approval(self):
        f = Fixture(self); f.approvalStore._history.clear()
        self.refused(f, 'INVALID_APPROVAL')
        self.assertFalse(replay(f.evidence).runs)
        f = Fixture(self)
        f.worker.cancelConfirms = False
        f.failpoint = lambda point: f.revoke() if point == 'WORKER:action' else None
        self.refused(f, 'APPROVAL_REVOKED', worker=1)
        self.assertEqual(replay(f.evidence).runs[f.run_id]['state'], 'CANCELLATION_PENDING')

    def test_02_identity_scope_and_spec_tampering(self):
        cases = [('repository', 'elsewhere/repo'), ('taskId', uid(99)), ('revision', 2),
                 ('baseSha', 'b' * 40), ('acceptanceCriteria', ['changed']), ('allowedPaths', ['file:scratch/a.txt'])]
        for key, value in cases:
            with self.subTest(key=key):
                f = Fixture(self); f.spec[key] = value
                self.refused(f)
        for key, value in [('taskDigest', 'f' * 64), ('ownerId', 'forged'), ('approvalId', uid(99))]:
            with self.subTest(envelope=key):
                f = Fixture(self); f.envelope[key] = value
                self.refused(f, 'INVALID_APPROVAL')

    def test_03_stale_base_at_all_boundaries(self):
        for point, starts in [(None, 0), ('WORKER:action', 1), ('VALIDATOR:confirmed', 1)]:
            with self.subTest(point=point):
                f = Fixture(self)
                def stale(name):
                    if name == point:
                        f.repositoryStore._state['baseSha'] = 'b' * 40
                if point is None:
                    f.repositoryStore._state['baseSha'] = 'b' * 40
                else:
                    f.failpoint = stale
                self.refused(f, 'STALE_BASE', worker=starts)

    def test_04_time_boundaries_and_expiry_during_worker(self):
        for wall, allowed in [('2026-10-23T08:59:59.999Z', False), ('2026-10-23T09:00:00.000Z', True),
                              ('2026-10-23T10:59:59.999Z', True), ('2026-10-23T11:00:00.000Z', False),
                              ('2026-10-23T11:00:00.001Z', False)]:
            with self.subTest(wall=wall):
                f = Fixture(self); f.clock.wall = wall
                if allowed:
                    f.run()
                else:
                    self.refused(f)
        f = Fixture(self)
        f.failpoint = lambda p: setattr(f.clock, 'wall', f.record['expiresAt']) if p == 'WORKER:action' else None
        self.refused(f, 'APPROVAL_EXPIRED', worker=1)
        f = Fixture(self); bad = dict(f.record, expiresAt=f.record['issuedAt'])
        with self.assertRaises(Refusal):
            f.approvalStore.put(bad)

    def test_05_revocation_cancel_and_store_down(self):
        for point, starts in [(None, 0), ('WORKER:action', 1), ('RESULT:append', 1), ('VALIDATOR:confirmed', 1)]:
            with self.subTest(point=point):
                f = Fixture(self)
                if point is None:
                    f.revoke()
                else:
                    f.failpoint = lambda name: f.revoke() if name == point else None
                self.refused(f, 'APPROVAL_REVOKED', worker=starts)
                if point == 'RESULT:append':
                    self.assertEqual(f.controller.localStatus, 'STOP_REQUESTED_UNVERIFIED')
                    self.assertEqual(recovery_decision(f.evidence, f.worker)['state'], 'RECOVERY_REQUIRED')
        f = Fixture(self)
        def outage(point):
            if point == 'WORKER:action':
                f.evidence.available = False
        f.failpoint = outage
        self.refused(f, 'STORE_UNAVAILABLE', worker=1)
        self.assertEqual(f.controller.localStatus, 'STOP_REQUESTED_UNVERIFIED')

    def test_06_credentials_have_no_model_capability(self):
        for channel in ('payload', 'env', 'accessibleFiles'):
            with self.subTest(channel=channel):
                f = Fixture(self)
                context = dict(payload={}, env={}, accessibleFiles={})
                context[channel] = {'syntheticCredential': 'test-only-not-a-secret'}
                with self.assertRaises(Refusal):
                    f.run(worker_context=context)
                self.assertEqual(f.worker.workerStarts, 0)
                self.assertEqual(f.controller.externalCalls, 0)

    def test_07_diff_and_snapshot_rejections(self):
        alterations = [lambda d: d.update(newMode='100755'), lambda d: d.update(newSha256='f' * 64),
                       lambda d: d.update(newContentBase64='%%%'), lambda d: d.update(path='../escape'),
                       lambda d: d.update(path='app/a.txt')]
        for alter in alterations:
            with self.subTest(alter=alter):
                f = Fixture(self); alter(f.diff[0]); self.refused(f, worker=1)
        f = Fixture(self); f.diff *= 2; self.refused(f, 'INVALID_DIFF', worker=1)
        f = Fixture(self); f.diff = [modify(raw=b'\xff')]; self.refused(f, 'INVALID_UTF8', worker=1)
        f = Fixture(self, files={'scratch/a.txt': b'a\n', 'scratch/X.txt': b'x'})
        f.diff = [modify(path='scratch/x.txt', op='ADD')]; self.refused(f, 'INVALID_PATH', worker=1)
        f = Fixture(self)
        def mutate(point):
            if point == 'WORKER:action':
                from types import MappingProxyType
                f.snapshot.files = MappingProxyType({'scratch/a.txt': b'tampered'})
        f.failpoint = mutate
        self.refused(f, 'SNAPSHOT_TAMPERED', worker=1)

    def test_08_binding_and_historical_receipt_after_revocation(self):
        f = Fixture(self); answer = f.run()
        p = answer['projection']
        w, v = p.worker_results[f.run_id], p.validations[f.run_id]
        self.assertNotEqual(w['operationId'], v['operationId'])
        self.assertEqual(w['payload']['workerResponseDigest'], v['payload']['validationReceipt']['workerResponseDigest'])
        f.revoke()
        historical = f.run()
        self.assertEqual(historical['historicalReceipt'], w['payload']['receipt'])
        self.assertEqual(f.worker.workerStarts, 1); self.assertEqual(f.controller.publisherCalls, 1)
        f.ops['VALIDATOR'] = uid(50)
        self.refused(f, 'IDEMPOTENCY_CONFLICT', worker=1, publisher=1)
        for field, value in [('runId', uid(90)), ('revision', 2), ('taskId', uid(90)), ('baseSha', 'b' * 40)]:
            f = Fixture(self)
            def forged(request):
                obj = dict(request, diffOps=f.diff, report='synthetic')
                obj[field] = value
                return dumps(obj)
            f.worker.response_factory = forged
            self.refused(f, 'INVALID_DIFF', worker=1)

    def test_09_stores_and_crash_boundaries_fail_closed(self):
        for store, flag in [('approvalStore', 'available'), ('repositoryStore', 'available'),
                            ('evidence', 'readable'), ('evidence', 'writable')]:
            with self.subTest(store=store, flag=flag):
                f = Fixture(self); setattr(getattr(f, store), flag, False)
                self.refused(f)
        for flag in ('available', 'readable', 'writable'):
            f = Fixture(self); setattr(f.evidence.checkpoint, flag, False); self.refused(f)
        for point in ('INTENT:append', 'INTENT:checkpoint', 'WORKER:before-action',
                      'WORKER:action', 'RESULT:append', 'RESULT:checkpoint'):
            with self.subTest(point=point):
                f = Fixture(self)
                def crash(name):
                    if name == point:
                        raise Crash(point)
                f.failpoint = crash
                with self.assertRaises(Crash):
                    f.run()
                projection = replay(f.evidence, allow_tail=True)
                confirmed = point != 'INTENT:append'
                self.assertEqual(projection.budgets.get(digest('spec', f.spec), {}).get('bytes', 0),
                                 4096 if confirmed else 0)
                f.failpoint = None
                if point != 'RESULT:checkpoint':
                    self.refused(f, 'RECOVERY_REQUIRED', worker=f.worker.workerStarts)
                else:
                    self.assertIn('historicalReceipt', f.run())
        f = Fixture(self); f.controller.trust = Trust('')
        self.refused(f, 'STORE_UNAVAILABLE')

    def test_10_worker_success_cannot_bypass_validator(self):
        f = Fixture(self); f.diff[0]['oldSha256'] = 'e' * 64
        self.refused(f, 'HASH_MISMATCH', worker=1)
        event = replay(f.evidence).validations[f.run_id]
        self.assertEqual(event['payload']['validationReceipt']['validationStatus'], 'REJECTED')
        self.assertEqual(event['payload']['workerResponseDigest'], None)

    def test_11_resource_limits_reservation_and_concurrency(self):
        for maximum in (2, 3, 4):
            f = Fixture(self, limits={'maxOperationsPerRun': maximum})
            if maximum < 3:
                self.refused(f, 'LIMIT_EXCEEDED', worker=1)
            else:
                f.run()
        for maximum in (1, 2, 3):
            f = Fixture(self, limits={'maxDiffLines': maximum})
            if maximum < 2:
                self.refused(f, 'LIMIT_EXCEEDED', worker=1)
            else:
                f.run()
        for elapsed in (999, 1000, 1001):
            f = Fixture(self)
            f.failpoint = lambda p: setattr(f.clock, 'elapsed', elapsed) if p == 'WORKER:action' else None
            if elapsed > 1000:
                self.refused(f, 'LIMIT_EXCEEDED', worker=1)
            else:
                f.run()
        # Raw response measured independently, including JSON overhead and Base64.
        sample = Fixture(self)
        request = {k: sample.spec[k] for k in ('protocolVersion', 'repository', 'taskId', 'revision', 'baseSha', 'snapshotDigest')}
        request.update(taskDigest=digest('spec', sample.spec), runId=sample.run_id, operationId=sample.ops['WORKER'])
        # Use whitespace padding so changing a limit/digest does not change serialized size.
        for delta in (-1, 0, 1):
            f = Fixture(self, limits={'maxTotalOutputBytesPerTask': 1024})
            base_response = f.response(dict(request, taskDigest=digest('spec', f.spec)))
            f.workerResponse = base_response + b' ' * (1024 + delta - len(base_response))
            if delta > 0:
                self.refused(f, 'LIMIT_EXCEEDED', worker=1)
            else:
                f.run()
            self.assertEqual(replay(f.evidence).budgets[digest('spec', f.spec)]['bytes'], 1024)
        f = Fixture(self, limits={'maxRunsPerTask': 1})
        with ThreadPoolExecutor(max_workers=2) as pool:
            futures = [pool.submit(f.run) for _ in range(2)]
            results = [x.result() for x in futures]
        self.assertEqual(f.worker.workerStarts, 1)
        self.assertTrue(any('historicalReceipt' in r for r in results))

    def test_12_happy_path_zero_remaining_and_independent_replay(self):
        f = Fixture(self); answer = f.run(); p = replay(f.evidence)
        self.assertEqual(p, answer['projection'])
        self.assertEqual(p.tasks[(digest('spec', f.spec), f.record['approvalId'])], f.expectedTaskState)
        self.assertEqual(p.runs[f.run_id]['state'], f.expectedRunState)
        self.assertEqual(p.budgets[digest('spec', f.spec)], f.confirmedBudget)
        self.assertFalse(p.pending)
        self.assertEqual(dict(answer['resultSnapshot'].files), {'scratch/a.txt': b'b\n'})
        self.assertEqual([e['eventType'] for e in p.events], f.expectedEvents)
        self.assertEqual(f.evidence.checkpoint.read()['rootHash'], p.events[-1]['eventHash'])
        self.assertEqual(f.controller.externalCalls, 0)
        f.approvalStore.available = f.repositoryStore.available = False
        self.assertEqual(replay(f.evidence), p)

    def test_13_forged_envelope_and_store_input(self):
        f = Fixture(self); f.envelope['status'] = 'ACTIVE'; self.refused(f, 'INVALID_SCHEMA')
        f = Fixture(self); record = dict(f.record, scope=['dir:scratch/', 'dir:scratch/'])
        with self.assertRaises(Refusal):
            f.approvalStore.put(record)
        f = Fixture(self); f.envelope['ownerId'] = 'other-owner'; self.refused(f, 'INVALID_APPROVAL')
        # Compromise of the independently trusted store is explicitly outside this model.

    def test_14_evidence_tampering_and_checkpoint_rollback(self):
        for mutation in ('rewrite', 'truncate', 'reorder', 'rehash', 'foreign', 'missing', 'stale', 'tail'):
            with self.subTest(mutation=mutation):
                f = Fixture(self); f.run()
                rows = f.evidence.path.read_bytes().splitlines()
                from tools.agent_mock_b.canonical import loads
                if mutation in ('rewrite', 'rehash', 'foreign'):
                    events = [loads(row) for row in rows]
                    if mutation == 'foreign':
                        events[0]['logId'] = uid(90)
                    else:
                        events[0]['payload']['reason'] = 'rewritten'
                    if mutation == 'rehash':
                        root = f.evidence.genesis
                        for event in events:
                            event['previousEventHash'] = root
                            event['eventHash'] = digest('evidence', {k: v for k, v in event.items() if k != 'eventHash'})
                            root = event['eventHash']
                    rows = [dumps(e) for e in events]
                elif mutation == 'truncate':
                    rows.pop()
                elif mutation == 'reorder':
                    rows[0], rows[1] = rows[1], rows[0]
                elif mutation == 'missing':
                    f.evidence.checkpoint.path.unlink()
                elif mutation == 'stale':
                    cp = f.evidence.checkpoint.read(); cp['sequenceNo'] -= 1
                    f.evidence.checkpoint.path.write_bytes(dumps(cp))
                elif mutation == 'tail':
                    rows.append(rows[-1])
                f.evidence.path.write_bytes(b'\n'.join(rows) + b'\n')
                with self.assertRaises(Refusal):
                    replay(f.evidence)

    def test_15_no_real_publish_and_no_direct_bypass(self):
        f = Fixture(self); f.spec['publishAllowed'] = True
        self.refused(f, 'REAL_PUBLISH_FORBIDDEN')
        f = Fixture(self)
        with self.assertRaises(Refusal):
            f.controller.publish_simulated(f.spec, f.envelope, f.run_id, f.ops['SIMULATED_PUBLISH'],
                                           (f.evidence.log_id, 1, '0' * 64), {}, f.snapshot, 0)
        self.assertEqual(f.controller.publisherCalls, 0)
        f = Fixture(self)
        def stop_after_validation(point):
            if point == 'VALIDATOR:confirmed':
                raise Crash('pause')
        f.failpoint = stop_after_validation
        with self.assertRaises(Crash):
            f.run()
        f.failpoint = None
        p = replay(f.evidence); event = p.validations[f.run_id]
        request = {k: f.spec[k] for k in ('protocolVersion', 'repository', 'taskId', 'revision', 'baseSha', 'snapshotDigest')}
        request.update(taskDigest=digest('spec', f.spec), runId=f.run_id, operationId=f.ops['WORKER'])
        from tools.agent_mock_b.canonical import loads
        obj = loads(f.response(request))
        for reference in [(event['logId'], event['sequenceNo'], '0' * 64), (uid(90), event['sequenceNo'], event['eventHash'])]:
            with self.assertRaises(Refusal):
                f.controller.publish_simulated(f.spec, f.envelope, f.run_id, f.ops['SIMULATED_PUBLISH'],
                                               reference, obj, Snapshot({'scratch/a.txt': b'b\n'}), 0)
        self.assertEqual(f.controller.publisherCalls, 0)
        self.assertNotIn('PUBLISHED', [e['eventType'] for e in p.events])
