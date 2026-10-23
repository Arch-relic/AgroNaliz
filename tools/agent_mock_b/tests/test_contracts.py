import copy
import json
import unittest

import rfc8785

from tools.agent_mock_b.canonical import Refusal, digest, dumps, loads
from tools.agent_mock_b.contracts import specification, timestamp, response, approval, receipt
from tools.agent_mock_b.evidence import event_schema
from test_acceptance_15 import Fixture, Crash


class Contracts(unittest.TestCase):
    def test_rfc_reference_vectors(self):
        self.assertEqual(rfc8785.dumps([333333333.33333329, 1e30, 4.50, 2e-3, 1e-27, -0.0]),
                         b'[333333333.3333333,1e+30,4.5,0.002,1e-27,0]')
        keys = ['\r', '1', '\u0080', 'ö', '€', '😀', 'דּ']
        self.assertEqual(list(json.loads(rfc8785.dumps(dict.fromkeys(reversed(keys), 0)))), keys)
        self.assertEqual(rfc8785.dumps('\u000f\n"\\'), b'"\\u000f\\n\\"\\\\"')
        self.assertEqual(rfc8785.dumps('é'), '"é"'.encode())
        self.assertNotEqual(rfc8785.dumps('é'), rfc8785.dumps('e\u0301'))

    def test_json_duplicate_unicode_float_nan_and_integer_bounds(self):
        for raw in (b'{"x":1,"x":2}', b'NaN', b'Infinity', b'1.0', b'-1', b'9007199254740992',
                    b'"\\ud800"', b'"\xff"', b'\xef\xbb\xbf{}'):
            with self.subTest(raw=raw), self.assertRaises(Refusal):
                loads(raw)
        self.assertEqual(loads(b'9007199254740991'), 9007199254740991)
        with self.assertRaises(Refusal):
            dumps(0.0)

    def test_specification_unknown_missing_fields_and_integer_bool(self):
        for key in ('revision',) + tuple(Fixture(self).spec['limits']):
            for value in (True, False, 0, -1, 1.0, 9007199254740992):
                with self.subTest(key=key, value=value):
                    f = Fixture(self); target = f.spec if key == 'revision' else f.spec['limits']
                    target[key] = value
                    with self.assertRaises(Refusal):
                        specification(f.spec)
        for mutation in ('unknown', 'missing', 'taskDigest'):
            f = Fixture(self)
            if mutation == 'missing':
                del f.spec['revision']
            else:
                f.spec[mutation] = 'x'
            with self.assertRaises(Refusal):
                specification(f.spec)

    def test_set_arrays_reject_duplicates_before_sorting(self):
        for key in ('allowedPaths', 'protectedPaths', 'allowedOperations', 'acceptanceCriteria'):
            f = Fixture(self); f.spec[key].append(f.spec[key][0])
            with self.subTest(key=key), self.assertRaisesRegex(Refusal, 'DUPLICATE_SET_VALUE'):
                specification(f.spec)
        f = Fixture(self); f.spec['allowedPaths'] = ['file:scratch/z', 'file:scratch/a']
        self.assertEqual(specification(f.spec)['allowedPaths'], ['file:scratch/a', 'file:scratch/z'])
        f = Fixture(self); f.spec['acceptanceCriteria'] = ['z', 'a']
        self.assertEqual(specification(f.spec)['acceptanceCriteria'], ['z', 'a'])

    def test_timestamp_strict_calendar_utc(self):
        for value in ('2026-02-29T00:00:00.000Z', '2026-10-23T10:00:60.000Z',
                      '2026-10-23T10:00:00Z', '2026-10-23T10:00:00.000+00:00',
                      '2026-13-01T00:00:00.000Z', '2026-10-23T10:00:00.0000Z'):
            with self.subTest(value=value), self.assertRaises(Refusal):
                timestamp(value)
        timestamp('2024-02-29T00:00:00.000Z')

    def test_amended_result_field_is_required_and_strict(self):
        f = Fixture(self); p = f.run()['projection']
        worker = p.worker_results[f.run_id]
        for value in (None, '', 'A' * 64, '0' * 63, True):
            obj = copy.deepcopy(worker); obj['payload']['workerResponseDigest'] = value
            with self.subTest(value=value), self.assertRaises(Refusal):
                event_schema(obj)
        obj = copy.deepcopy(worker); del obj['payload']['workerResponseDigest']
        with self.assertRaises(Refusal):
            event_schema(obj)
        for event in p.events:
            if event['eventType'] == 'RESULT' and event['payload']['operationKind'] != 'WORKER':
                event_schema(event)
                obj = copy.deepcopy(event); obj['payload']['workerResponseDigest'] = '0' * 64
                with self.assertRaises(Refusal):
                    event_schema(obj)
        obj = copy.deepcopy(worker); obj['outcome'] = 'REJECTED'
        obj['payload']['workerResponseDigest'] = None
        event_schema(obj)
        obj['payload']['workerResponseDigest'] = '0' * 64
        with self.assertRaises(Refusal):
            event_schema(obj)

    def test_operation_receipt_and_worker_response_do_not_gain_fields(self):
        f = Fixture(self); p = f.run()['projection']; w = p.worker_results[f.run_id]
        r = copy.deepcopy(w['payload']['receipt']); r['workerResponseDigest'] = '0' * 64
        with self.assertRaises(Refusal):
            receipt(r)
        raw = f.worker.lookup(f.ops['WORKER'])['raw']; obj = loads(raw)
        obj['workerResponseDigest'] = '0' * 64
        with self.assertRaises(Refusal):
            response(obj)

    def test_unknown_protocol_enum_and_event_fields(self):
        f = Fixture(self); event = f.run()['projection'].events[0]
        for key, value in [('protocolVersion', 'future'), ('eventType', 'PUBLISHED'),
                           ('eventType', 'PUBLISH_REQUESTED'), ('outcome', {'ok': True}), ('actor', 'github')]:
            obj = copy.deepcopy(event); obj[key] = value
            with self.subTest(key=key, value=value), self.assertRaises(Refusal):
                event_schema(obj)
        obj = copy.deepcopy(event); obj['payload']['unknown'] = True
        with self.assertRaises(Refusal):
            event_schema(obj)

    def test_domain_separation_and_nul(self):
        import hashlib
        obj = {'a': 1}
        self.assertEqual(digest('spec', obj), hashlib.sha256(b'agronaliz.mock-b/1/spec\0{"a":1}').hexdigest())
        self.assertNotEqual(digest('spec', obj), digest('operation', obj))
