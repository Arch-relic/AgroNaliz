"""Independent application of a restricted structured diff to copied bytes."""
import base64
import binascii
from difflib import SequenceMatcher

from . import PROTOCOL
from .canonical import digest, require, sha, Refusal
from .contracts import DENY, IDENTITY, diffop, matches, response
from .snapshot import Snapshot, text


def protected(name, selectors):
    # Existing identity/key/signing/policy paths remain denied independent of spec.
    parts = name.lower().split('/')
    sensitive = any(any(word in part for word in ('secret', 'credential', 'signing', 'policy', 'identity'))
                    or part.endswith(('.pem', '.key', '.jks', '.keystore')) for part in parts)
    return sensitive or any(name == p.rstrip('/') or (p.endswith('/') and name.startswith(p)) for p in DENY) or matches(name, selectors)


def lines(raw):
    return text(raw).replace('\r\n', '\n').replace('\r', '\n').splitlines(keepends=True)


def apply(spec, snapshot, ops):
    snapshot.verify()
    require(type(ops) is list, 'INVALID_DIFF')
    require(len(ops) <= spec['limits']['maxDiffFiles'], 'LIMIT_EXCEEDED')
    result = dict(snapshot.files)
    seen, pairs, total_bytes = set(), [], 0
    for op in ops:
        diffop(op)
        name = op['path']
        require(name not in seen, 'INVALID_DIFF'); seen.add(name)
        require(not protected(name, spec['protectedPaths']), 'PROTECTED_PATH')
        require(matches(name, spec['allowedPaths']), 'INVALID_PATH')
        require(op['op'] in spec['allowedOperations'], 'INVALID_DIFF')
        exists = name in snapshot.files
        require(exists == (op['op'] != 'ADD'), 'INVALID_DIFF')
        old = snapshot.files.get(name, b'')
        if exists:
            require(sha(old) == op['oldSha256'], 'HASH_MISMATCH')
        if op['op'] == 'DELETE':
            new = b''
            del result[name]
        else:
            encoded = op['newContentBase64']
            remaining = 2097152 - total_bytes - len(old)
            require(remaining >= 0 and len(encoded) <= 4 * ((remaining + 2) // 3), 'LIMIT_EXCEEDED')
            try:
                new = base64.b64decode(encoded, validate=True)
            except (ValueError, binascii.Error):
                raise Refusal('INVALID_DIFF') from None
            require(base64.b64encode(new).decode() == encoded, 'INVALID_DIFF')
            require(len(new) <= remaining, 'LIMIT_EXCEEDED')
            text(new)
            require(sha(new) == op['newSha256'], 'HASH_MISMATCH')
            result[name] = new
        total_bytes += len(old) + len(new)
        require(total_bytes <= 2097152, 'LIMIT_EXCEEDED')
        a, b = lines(old), lines(new)
        require(len(a) <= 20000 and len(b) <= 20000, 'LIMIT_EXCEEDED')
        pairs.append((a, b))
    # All resource bounds precede every potentially expensive SequenceMatcher.
    changed = 0
    for a, b in pairs:
        for tag, i, j, k, m in SequenceMatcher(None, a, b, autojunk=False).get_opcodes():
            if tag != 'equal':
                changed += (j-i) + (m-k)
    require(changed <= spec['limits']['maxDiffLines'], 'LIMIT_EXCEEDED')
    return Snapshot(result)


def validate(spec, task_digest, snapshot, obj, worker_digest, run_id, worker_op, validator_op):
    response(obj)
    expected = {key: spec[key] for key in ('protocolVersion', 'repository', 'taskId', 'revision', 'baseSha', 'snapshotDigest')}
    expected.update(taskDigest=task_digest, runId=run_id, operationId=worker_op)
    require(all(obj[k] == v for k, v in expected.items()), 'INVALID_DIFF')
    require(digest('worker-response', obj) == worker_digest, 'HASH_MISMATCH')
    result = apply(spec, snapshot, obj['diffOps'])
    receipt = {k: obj[k] for k in IDENTITY}
    receipt.update(operationId=validator_op, workerResponseDigest=worker_digest,
                   diffDigest=digest('diff', {'diffOps': obj['diffOps']}), resultSnapshotDigest=result.digest,
                   validationStatus='PASSED', validatorVersion=PROTOCOL)
    return receipt, result
