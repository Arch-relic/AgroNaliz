"""Strict wire schemas. Unknown fields are rejected recursively."""
import copy
import re
from datetime import datetime
from uuid import UUID

from . import PROTOCOL
from .canonical import domain_json, require

REPOSITORY = 'Arch-relic/AgroNaliz'
DENY = ('.git/', '.github/', 'app/', 'gradle/', 'gradlew', 'gradlew.bat',
        'settings.gradle', 'settings.gradle.kts', 'build.gradle', 'build.gradle.kts',
        'local.properties', 'docs/codex-handoff.md', 'agent_core/', 'tests/agent_core/',
        'docs/agent-core.md', 'docs/agent-core-security-audit.md', 'docs/agent-identity-v2/')
LIMITS = ('maxRunsPerTask', 'maxOperationsPerRun', 'maxDiffFiles', 'maxDiffLines',
          'maxOutputBytes', 'maxWallDurationMs', 'maxTotalOutputBytesPerTask')
IDENTITY = ('protocolVersion', 'repository', 'taskId', 'revision', 'taskDigest',
            'baseSha', 'snapshotDigest', 'runId', 'operationId')


def fields(obj, names, optional=()):
    domain_json(obj)
    require(type(obj) is dict)
    require(set(names) <= obj.keys() <= set(names) | set(optional))


def integer(value, minimum=0):
    require(type(value) is int and minimum <= value <= 9007199254740991)


def string(value, low=1, high=4096):
    require(type(value) is str and low <= len(value) <= high)


def hexstr(value, length=64):
    require(type(value) is str and re.fullmatch('[0-9a-f]{' + str(length) + '}', value) is not None)


def uuid(value):
    string(value)
    try:
        require(str(UUID(value)) == value)
    except (ValueError, AttributeError):
        require(False)


def timestamp(value):
    require(type(value) is str and re.fullmatch(r'\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d{3}Z', value) is not None)
    try:
        return datetime.strptime(value, '%Y-%m-%dT%H:%M:%S.%fZ')
    except ValueError:
        require(False)


def path(value):
    require(type(value) is str and re.fullmatch('[A-Za-z0-9._/-]+', value) is not None, 'INVALID_PATH')
    require(all(p not in ('', '.', '..') for p in value.split('/')), 'INVALID_PATH')
    return value


def selector(value):
    string(value)
    if value.startswith('file:'):
        path(value[5:])
    elif value.startswith('dir:') and value.endswith('/'):
        path(value[4:-1])
    else:
        require(False, 'INVALID_PATH')


def matches(value, selections):
    return any((s.startswith('file:') and value == s[5:]) or
               (s.startswith('dir:') and value.startswith(s[4:])) for s in selections)


def set_array(value, check):
    require(type(value) is list)
    for item in value:
        check(item)
    require(len(value) == len(set(value)), 'DUPLICATE_SET_VALUE')
    return sorted(value)


def specification(value):
    value = copy.deepcopy(value)
    fields(value, ('protocolVersion', 'repository', 'taskId', 'revision', 'baseSha',
                   'snapshotDigest', 'allowedPaths', 'protectedPaths', 'allowedOperations',
                   'acceptanceCriteria', 'limits', 'workerKind', 'publishAllowed'))
    require(value['protocolVersion'] == PROTOCOL)
    require(value['repository'] == REPOSITORY)
    uuid(value['taskId']); integer(value['revision'], 1)
    hexstr(value['baseSha'], 40); hexstr(value['snapshotDigest'])
    require(value['workerKind'] == 'mock')
    require(value['publishAllowed'] is False, 'REAL_PUBLISH_FORBIDDEN')
    for key in ('allowedPaths', 'protectedPaths'):
        value[key] = set_array(value[key], selector)
    expected = {'dir:' + p if p.endswith('/') else 'file:' + p for p in DENY}
    require(expected <= set(value['protectedPaths']), 'PROTECTED_PATH')
    def operation(item):
        require(item in ('ADD', 'MODIFY', 'DELETE'))
    value['allowedOperations'] = set_array(value['allowedOperations'], operation)
    criteria = value['acceptanceCriteria']
    require(type(criteria) is list and len(criteria) > 0)
    for item in criteria:
        string(item, 1, 256)
    require(len(criteria) == len(set(criteria)), 'DUPLICATE_SET_VALUE')
    fields(value['limits'], LIMITS)
    for limit in value['limits'].values():
        integer(limit, 1)
    return value


def envelope(obj):
    fields(obj, ('approvalId', 'taskDigest', 'ownerId'))
    uuid(obj['approvalId']); hexstr(obj['taskDigest']); string(obj['ownerId'])


def approval(obj):
    fields(obj, ('approvalId', 'taskId', 'revision', 'taskDigest', 'ownerId',
                 'issuedAt', 'expiresAt', 'scope', 'status', 'storeVersion'))
    uuid(obj['approvalId']); uuid(obj['taskId']); integer(obj['revision'], 1)
    hexstr(obj['taskDigest']); string(obj['ownerId']); integer(obj['storeVersion'], 1)
    require(obj['status'] in ('ACTIVE', 'REVOKED', 'DENIED'))
    require(timestamp(obj['issuedAt']) < timestamp(obj['expiresAt']), 'INVALID_APPROVAL')
    set_array(obj['scope'], selector)


def repository(obj):
    fields(obj, ('repository', 'baseSha', 'snapshotDigest', 'storeVersion'))
    require(obj['repository'] == REPOSITORY)
    hexstr(obj['baseSha'], 40); hexstr(obj['snapshotDigest']); integer(obj['storeVersion'], 1)


def identity(obj):
    require(obj['protocolVersion'] == PROTOCOL and obj['repository'] == REPOSITORY)
    uuid(obj['taskId']); integer(obj['revision'], 1); hexstr(obj['taskDigest'])
    hexstr(obj['baseSha'], 40); hexstr(obj['snapshotDigest'])
    uuid(obj['runId']); uuid(obj['operationId'])


def diffop(obj):
    fields(obj, ('op', 'path', 'oldSha256', 'newSha256', 'newContentBase64', 'newMode'))
    require(obj['op'] in ('ADD', 'MODIFY', 'DELETE'), 'INVALID_DIFF')
    path(obj['path'])
    if obj['op'] == 'ADD':
        require(obj['oldSha256'] is None, 'INVALID_DIFF')
    else:
        hexstr(obj['oldSha256'])
    if obj['op'] == 'DELETE':
        require(all(obj[k] is None for k in ('newSha256', 'newContentBase64', 'newMode')), 'INVALID_DIFF')
    else:
        hexstr(obj['newSha256']); string(obj['newContentBase64'], 0, 4 * 2097152)
        require(obj['newMode'] == '100644', 'INVALID_DIFF')


def response(obj):
    fields(obj, IDENTITY + ('diffOps', 'report'))
    identity(obj)
    require(type(obj['diffOps']) is list)
    for op in obj['diffOps']:
        diffop(op)
    string(obj['report'], 0, 4096)


def validation(obj):
    fields(obj, IDENTITY + ('workerResponseDigest', 'diffDigest', 'resultSnapshotDigest',
                            'validationStatus', 'validatorVersion'))
    identity(obj)
    for key in ('workerResponseDigest', 'diffDigest', 'resultSnapshotDigest'):
        hexstr(obj[key])
    require(obj['validationStatus'] in ('PASSED', 'REJECTED'))
    require(obj['validatorVersion'] == PROTOCOL)


def receipt(obj):
    fields(obj, ('taskId', 'runId', 'operationId', 'inputDigest', 'phase', 'outcome'), ('evidenceSequenceNo',))
    for key in ('taskId', 'runId', 'operationId'):
        uuid(obj[key])
    hexstr(obj['inputDigest'])
    require(obj['phase'] in ('WORKER', 'VALIDATOR', 'SIMULATED_PUBLISH'))
    require(obj['outcome'] in ('OK', 'REJECTED', 'UNKNOWN'))
    if 'evidenceSequenceNo' in obj:
        integer(obj['evidenceSequenceNo'], 1)
