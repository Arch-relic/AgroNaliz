import copy
from concurrent.futures import ThreadPoolExecutor
import unittest

from tools.agent_mock_b.approval_store import Trust
from tools.agent_mock_b.canonical import Refusal, digest, dumps, loads
from tools.agent_mock_b.evidence import Evidence
from tools.agent_mock_b.recovery import replay, recovery_decision
from tools.agent_mock_b.snapshot import Snapshot
from tools.agent_mock_b.budget import reservation, action_limit
from test_acceptance_15 import Fixture, Crash, uid


def reseal(f, events):
    """Test-only malformed semantic journal with a matching synthetic checkpoint."""
    root = f.evidence.genesis
    for event in events:
        event['previousEventHash'] = root
        event['eventHash'] = digest('evidence', {k: v for k, v in event.items() if k != 'eventHash'})
        root = event['eventHash']
    f.evidence.path.write_bytes(b'\n'.join(dumps(e) for e in events) + b'\n')
    checkpoint = dict(logId=f.evidence.log_id, sequenceNo=len(events), rootHash=root, storeVersion=len(events))
    # This is a fixture setup, not the production checkpoint update path.
    f.evidence.checkpoint.path.write_bytes(dumps(checkpoint))
    f.evidence.checkpoint._anchor = copy.deepcopy(checkpoint)


class StateRecovery(unittest.TestCase):
    def test_replay_uses_checkpointed_worker_digest_only(self):
        f = Fixture(self); answer = f.run()
        expected = answer['projection']
        f.controller = None; f.worker = None; f.spec = None; f.envelope = None
        self.assertEqual(replay(f.evidence), expected)
        self.assertEqual(expected.worker_results[uid(10)]['payload']['workerResponseDigest'],
                         expected.validations[uid(10)]['payload']['validationReceipt']['workerResponseDigest'])

    def test_missing_mismatched_and_foreign_worker_results(self):
        for mutation in ('missing_digest', 'different_digest', 'foreign_run', 'foreign_input', 'foreign_operation', 'duplicate_result'):
            with self.subTest(mutation=mutation):
                f = Fixture(self); events = f.run()['projection'].events
                worker = events[2]
                if mutation == 'missing_digest':
                    del worker['payload']['workerResponseDigest']
                elif mutation == 'different_digest':
                    worker['payload']['workerResponseDigest'] = '0' * 64
                elif mutation == 'foreign_run':
                    worker['runId'] = worker['payload']['runId'] = uid(99)
                elif mutation == 'foreign_input':
                    worker['inputDigest'] = worker['payload']['inputDigest'] = '0' * 64
                elif mutation == 'foreign_operation':
                    worker['operationId'] = worker['payload']['operationId'] = uid(99)
                else:
                    events[3] = copy.deepcopy(worker)
                    events[3]['sequenceNo'] = 4; events[3]['eventId'] = uid(500)
                reseal(f, events)
                with self.assertRaises(Refusal):
                    replay(f.evidence)
                self.assertEqual(f.controller.publisherCalls, 1)  # Earlier valid run only, no new action.

    def test_validation_binding_forgery_and_rejected_receipt(self):
        for key, value in [('workerResponseDigest', 'f' * 64), ('taskDigest', 'f' * 64),
                           ('runId', uid(99)), ('baseSha', 'b' * 40), ('operationId', uid(11))]:
            f = Fixture(self); events = f.run()['projection'].events
            events[4]['payload']['validationReceipt'][key] = value
            reseal(f, events)
            with self.subTest(key=key), self.assertRaises(Refusal):
                replay(f.evidence)
        f = Fixture(self); f.diff[0]['oldSha256'] = 'f' * 64
        with self.assertRaises(Refusal):
            f.run()
        p = replay(f.evidence); v = p.validations[f.run_id]
        from tools.agent_mock_b.simulated_publisher import verify_link
        with self.assertRaises(Refusal):
            verify_link(p, (v['logId'], v['sequenceNo'], v['eventHash']), f.spec, digest('spec', f.spec),
                        f.run_id, v['payload']['validationReceipt']['workerResponseDigest'],
                        v['payload']['validationReceipt']['diffDigest'], v['payload']['validationReceipt']['resultSnapshotDigest'])
        self.assertEqual(f.controller.publisherCalls, 0)

    def test_cas_loser_consumes_no_budget_and_starts_nothing(self):
        f = Fixture(self)
        original = f.evidence.checkpoint.compare_and_set
        def loser(old, new):
            if new['sequenceNo'] == 2:
                raise Refusal('INTEGRITY_FAILED')
            return original(old, new)
        f.evidence.checkpoint.compare_and_set = loser
        with self.assertRaises(Refusal):
            f.run()
        self.assertEqual(f.worker.workerStarts, 0)
        self.assertFalse(replay(f.evidence, allow_tail=True).budgets)
        f.evidence.checkpoint.compare_and_set = original

    def test_crashes_every_stage_never_retry_unknown_action(self):
        points = ('VALIDATOR:before-action', 'VALIDATOR:action', 'VALIDATOR:confirmed',
                  'SIMULATED_PUBLISH:before-action', 'SIMULATED_PUBLISH:action')
        for point in points:
            f = Fixture(self)
            def fail(name):
                if name == point:
                    raise Crash(name)
            f.failpoint = fail
            with self.subTest(point=point), self.assertRaises(Crash):
                f.run()
            starts, calls = f.worker.workerStarts, f.controller.publisherCalls
            f.failpoint = None
            answer = f.run()
            self.assertIn('historicalReceipt', answer)
            self.assertEqual((f.worker.workerStarts, f.controller.publisherCalls), (starts, calls))
            self.assertEqual(replay(f.evidence).budgets[digest('spec', f.spec)]['bytes'], 4096)

    def test_recovery_unknown_tail_and_independent_ledger(self):
        for point, expected in [('INTENT:append', {}), ('INTENT:checkpoint', {uid(11): 'OUTCOME_UNKNOWN'}),
                                ('WORKER:action', {uid(11): 'RESULT_AVAILABLE'})]:
            f = Fixture(self)
            def fail(name):
                if name == point:
                    raise Crash(name)
            f.failpoint = fail
            with self.assertRaises(Crash):
                f.run()
            decision = recovery_decision(f.evidence, f.worker)
            self.assertEqual(decision['state'], 'RECOVERY_REQUIRED')
            self.assertEqual(decision['operations'], expected)
            self.assertEqual(f.controller.publisherCalls, 0)
        f.worker.available = False
        with self.assertRaises(Refusal):
            recovery_decision(f.evidence, f.worker)

    def test_protective_cancel_needs_no_approval_and_no_fake_confirmation(self):
        f = Fixture(self)
        def fail(name):
            if name == 'WORKER:before-action':
                raise Crash(name)
        f.failpoint = fail
        with self.assertRaises(Crash):
            f.run()
        f.revoke(); f.worker.cancelConfirms = False
        f.controller.stop(f.run_id, 'synthetic-recovery')
        self.assertEqual(replay(f.evidence).runs[f.run_id]['state'], 'CANCELLATION_PENDING')
        f.worker.cancelConfirms = True
        f.controller.stop(f.run_id, 'synthetic-recovery-confirmed')
        self.assertEqual(replay(f.evidence).runs[f.run_id]['state'], 'CANCELLED')
        self.assertEqual(f.worker.workerStarts, 0)

    def test_budget_is_shared_across_approvals_and_failures(self):
        f = Fixture(self, limits={'maxRunsPerTask': 3, 'maxOutputBytes': 4096, 'maxTotalOutputBytesPerTask': 5000})
        f.diff[0]['oldSha256'] = 'f' * 64  # A confirmed rejected validation, not an unknown action.
        for attempt, expected in [(0, 4096), (1, 5000)]:
            if attempt:
                f.record = dict(f.record, approvalId=uid(20), storeVersion=1)
                f.approvalStore.put(f.record)
                f.envelope = {k: f.record[k] for k in ('approvalId', 'taskDigest', 'ownerId')}
                f.run_id = uid(30); f.ops = dict(WORKER=uid(31), VALIDATOR=uid(32), SIMULATED_PUBLISH=uid(33))
            with self.assertRaisesRegex(Refusal, 'HASH_MISMATCH'):
                f.run()
            self.assertEqual(replay(f.evidence).budgets[digest('spec', f.spec)]['bytes'], expected)
        f.run_id = uid(40); f.ops = dict(WORKER=uid(41), VALIDATOR=uid(42), SIMULATED_PUBLISH=uid(43))
        with self.assertRaisesRegex(Refusal, 'BUDGET_EXHAUSTED'):
            f.run()
        self.assertEqual(f.worker.workerStarts, 2)

    def test_run_limit_and_last_slot_concurrent_distinct_runs(self):
        f = Fixture(self, limits={'maxRunsPerTask': 1, 'maxTotalOutputBytesPerTask': 20000})
        f.diff[0]['oldSha256'] = 'f' * 64
        def attempt(number):
            try:
                f.controller.execute(f.spec, f.envelope, uid(number),
                    dict(WORKER=uid(number+1), VALIDATOR=uid(number+2), SIMULATED_PUBLISH=uid(number+3)))
            except Refusal as error:
                return error.code
        with ThreadPoolExecutor(max_workers=2) as pool:
            answers = list(pool.map(attempt, (100, 200)))
        self.assertCountEqual(answers, ['HASH_MISMATCH', 'BUDGET_EXHAUSTED'])
        self.assertEqual(f.worker.workerStarts, 1)
        self.assertEqual(replay(f.evidence).budgets[digest('spec', f.spec)], {'runs': 1, 'bytes': 4096})

    def test_every_limit_below_at_above(self):
        # Pure resource decisions supplement controller end-to-end cases.
        f = Fixture(self); limits = dict(f.spec['limits'])
        for used, allowed in [(2, True), (3, False), (4, False)]:
            if allowed:
                reservation(limits, used, 0)
            else:
                with self.assertRaises(Refusal):
                    reservation(limits, used, 0)
        for reserved, allowed in [(4095, True), (4096, False), (4097, False)]:
            if allowed:
                self.assertEqual(reservation(limits, 0, reserved), (1, 1))
            else:
                with self.assertRaises(Refusal):
                    reservation(limits, 0, reserved)
        for number in (0, 1, 2):
            from tools.agent_mock_b.diff_validator import apply
            from test_acceptance_15 import modify
            f = Fixture(self, limits={'maxDiffFiles': 1})
            ops = [modify(op='ADD', path='scratch/new' + str(i)) for i in range(number)]
            if number <= 1:
                apply(f.spec, f.snapshot, ops)
            else:
                with self.assertRaises(Refusal):
                    apply(f.spec, f.snapshot, ops)

    def test_fresh_log_cannot_reset_a_bound_task_budget(self):
        f = Fixture(self); f.run()
        import tempfile
        with tempfile.TemporaryDirectory() as other:
            f.controller.evidence = Evidence(other, uid(1))
            with self.assertRaisesRegex(Refusal, 'INTEGRITY_FAILED'):
                f.run()
        self.assertEqual(f.worker.workerStarts, 1)

    def test_store_write_failures_and_checkpoint_rollback(self):
        f = Fixture(self); f.approvalStore.available = False
        with self.assertRaisesRegex(Refusal, 'STORE_UNAVAILABLE'):
            f.approvalStore.put(f.record)
        f = Fixture(self); cp = f.evidence.checkpoint.read()
        f.run()
        with self.assertRaises(Refusal):
            f.evidence.checkpoint.compare_and_set(cp, dict(cp, sequenceNo=1, storeVersion=1))
        # The trusted anchor rejects a rollback even if both sequence and root are plausible.
        f.evidence.checkpoint.path.write_bytes(dumps(cp))
        with self.assertRaisesRegex(Refusal, 'INTEGRITY_FAILED'):
            replay(f.evidence)

    def test_max_output_bytes_below_exact_and_above(self):
        for delta in (-1, 0, 1):
            f = Fixture(self, limits={'maxOutputBytes': 1024, 'maxTotalOutputBytesPerTask': 10000})
            def response(request):
                raw = dumps(dict(request, diffOps=f.diff, report='synthetic'))
                return raw + b' ' * (1024 + delta - len(raw))
            f.worker.response_factory = response
            if delta > 0:
                with self.assertRaisesRegex(Refusal, 'LIMIT_EXCEEDED'):
                    f.run()
                self.assertEqual(f.controller.publisherCalls, 0)
            else:
                f.run()
            self.assertEqual(replay(f.evidence).budgets[digest('spec', f.spec)]['bytes'], 1024)

    def test_outage_at_each_action_boundary_prevents_publication(self):
        for point in ('WORKER:before-action', 'WORKER:action', 'VALIDATOR:before-action', 'VALIDATOR:confirmed'):
            for store, flag in [('approvalStore', 'available'), ('repositoryStore', 'available'),
                                ('evidence', 'readable'), ('evidence', 'writable'),
                                ('checkpoint', 'readable'), ('checkpoint', 'writable')]:
                with self.subTest(point=point, store=store, flag=flag):
                    f = Fixture(self)
                    target = f.evidence.checkpoint if store == 'checkpoint' else getattr(f, store)
                    def outage(name):
                        if name == point:
                            setattr(target, flag, False)
                    f.failpoint = outage
                    with self.assertRaises(Refusal):
                        f.run()
                    self.assertEqual(f.controller.publisherCalls, 0)
                    self.assertGreater(f.worker.cancelRequests, 0)

    def test_publisher_checks_changed_diff_result_and_revoked_historical_receipt(self):
        f = Fixture(self)
        def pause(point):
            if point == 'VALIDATOR:confirmed':
                raise Crash(point)
        f.failpoint = pause
        with self.assertRaises(Crash):
            f.run()
        f.failpoint = None
        p = replay(f.evidence); event = p.validations[f.run_id]
        ref = (event['logId'], event['sequenceNo'], event['eventHash'])
        obj = loads(f.worker.lookup(f.ops['WORKER'])['raw'])
        forged = copy.deepcopy(obj); forged['report'] = 'different response'
        for response, result in [(forged, Snapshot({'scratch/a.txt': b'b\n'})),
                                 (obj, Snapshot({'scratch/a.txt': b'forged'}))]:
            with self.assertRaises(Refusal):
                f.controller.publish_simulated(f.spec, f.envelope, f.run_id, f.ops['SIMULATED_PUBLISH'], ref, response, result, 0)
        self.assertEqual(f.controller.publisherCalls, 0)
        result = Snapshot({'scratch/a.txt': b'b\n'})
        answer = f.controller.publish_simulated(f.spec, f.envelope, f.run_id, f.ops['SIMULATED_PUBLISH'], ref, obj, result, 0)
        f.revoke()
        historical = f.controller.publish_simulated(f.spec, f.envelope, f.run_id, f.ops['SIMULATED_PUBLISH'], ref, obj, result, 0)
        self.assertEqual(historical['historicalReceipt'], answer['receipt'])
        self.assertEqual(f.controller.publisherCalls, 1)

    def test_revocation_after_intent_append_does_not_confirm_reservation(self):
        f = Fixture(self)
        f.failpoint = lambda name: f.revoke() if name == 'INTENT:append' else None
        with self.assertRaisesRegex(Refusal, 'APPROVAL_REVOKED'):
            f.run()
        self.assertEqual(f.worker.workerStarts, 0)
        self.assertEqual(replay(f.evidence, allow_tail=True).budgets, {})

    def test_historical_receipt_ignores_current_external_store_availability(self):
        f = Fixture(self); f.run()
        f.approvalStore.available = f.repositoryStore.available = False
        self.assertIn('historicalReceipt', f.run())
        self.assertEqual(f.worker.workerStarts, 1)
        self.assertEqual(f.controller.publisherCalls, 1)
