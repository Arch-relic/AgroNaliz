import copy
import inspect
from pathlib import Path
import socket
import unittest
from unittest.mock import patch
from agent_core.core import authorize, digest, validate
from agent_core.v2.model import Rejected, app_task, document
from test_core import task as v1_task
from v2_fakes import Harness


class V2CompatibilityTests(unittest.TestCase):
    def test_v1_unchanged(self):
        task=v1_task();before=copy.deepcopy(task);old_hash=digest(task)
        validate(task)
        with self.assertRaisesRegex(Rejected,'V1_APP_EXECUTION_FORBIDDEN'):app_task(task)
        self.assertEqual(before,task);self.assertEqual(old_hash,digest(task))

    def test_v1_grant_not_transferred(self):
        from test_core import CoreTests
        case=CoreTests();case.setUp();before=copy.deepcopy(case.t)
        validate(case.t,api=case.api)
        h=Harness();r=__import__('agent_core.v2.controller',fromlist=['OfflineController']).OfflineController(
            h.transport,h.clock,h.anchors,h.credentials).simulate(case.t,'test',h.execution)
        self.assertEqual('V1_APP_EXECUTION_FORBIDDEN',r.reason)
        self.assertEqual([],h.transport.calls);self.assertEqual([],h.credentials.acquisitions)
        self.assertEqual(before,case.t)

    def test_v1_api_not_extended(self):
        self.assertEqual(['task','operation'],list(inspect.signature(authorize).parameters))

    def test_v2_rejected_by_v1(self):
        with self.assertRaises(ValueError):validate(Harness().task)

    def test_no_network_even_when_credentials_present(self):
        with (patch.object(socket,'socket',side_effect=AssertionError('Network forbidden')),
              patch.dict('os.environ',{'GH_TOKEN':'CANARY-NOT-A-CREDENTIAL','GITHUB_TOKEN':'CANARY'},clear=True)):
            h=Harness();self.assertEqual('allowed',h.run().outcome)

    def test_no_entrypoint_or_adapter(self):
        root=Path(__file__).parents[2]/'agent_core'/'v2'
        self.assertFalse((root/'__main__.py').exists())
        import ast
        forbidden={'os','subprocess','socket','urllib','requests','jwt','http','importlib'}
        for path in root.glob('*.py'):
            tree=ast.parse(path.read_text())
            for node in ast.walk(tree):
                if isinstance(node,(ast.Import,ast.ImportFrom)):
                    names=[a.name for a in node.names] if isinstance(node,ast.Import) else [node.module]
                    self.assertTrue(all(n.split('.')[0] not in forbidden for n in names),path)
