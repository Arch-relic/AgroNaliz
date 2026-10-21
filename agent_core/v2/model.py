"""Strict v2 documents; existing v1 contracts are never reinterpreted."""
from datetime import datetime, timezone
import json
from pathlib import Path
from jsonschema import Draft202012Validator, FormatChecker
from agent_core.core import digest, load_json


REASON_CODES = frozenset({
    'APPEND_ONLY_VIOLATION',
    'APPROVAL_CONTEXT_MISMATCH',
    'APPROVAL_POLICY_MISMATCH',
    'APPROVAL_REPLAY',
    'APPROVAL_REQUIRED',
    'APPROVAL_TASK_MISMATCH',
    'APPROVAL_TIME_INVALID',
    'APPROVAL_TITLE_MISMATCH',
    'CLEANUP_FAILED',
    'CLOCK_SKEW',
    'CONSENT_DURATION_INVALID',
    'CONSENT_NOT_CURRENT',
    'CONTEXT_HASH_MISMATCH',
    'CONTEXT_IDENTITY_MISMATCH',
    'CONTEXT_SPEC_MISMATCH',
    'CONTROLLER_RUN_EXPIRED',
    'DECISION_INVALID',
    'DUPLICATE_REVISION',
    'EXECUTION_CONTEXT_MISMATCH',
    'EXECUTOR_BINDING_MISMATCH',
    'HISTORY_CONTRADICTION',
    'HISTORY_TIME_ORDER',
    'IMMUTABLE_IDENTITY_CHANGED',
    'IMMUTABLE_REQUEST_MISMATCH',
    'INDEPENDENT_EVIDENCE_REQUIRED',
    'INSTALLATION_IDENTITY_MISMATCH',
    'INSTALLATION_SCOPE_MISMATCH',
    'INSTALLATION_UNAVAILABLE',
    'INVALID_JSON',
    'INVALID_JSON_KEY',
    'INVALID_JSON_TYPE',
    'INVALID_TIME',
    'LEASE_EXPIRED',
    'LEASE_INVALID',
    'LEASE_SCOPE_MISMATCH',
    'LIVE_CREDENTIALS_DISABLED',
    'MAIN_CHANGED',
    'MAIN_REF_INVALID',
    'OFFLINE_SMOKE_VERIFIED',
    'OPERATION_FORBIDDEN',
    'OWNER_IDENTITY_REQUIRED',
    'PROTOCOL_VERSION_REQUIRED',
    'REGISTRY_BINDING_MISMATCH',
    'REQUEST_NONCE_REUSED',
    'REQUEST_OUTSIDE_AWAITING',
    'REQUEST_POSITION_MISMATCH',
    'REVISION_REQUIRES_APPROVAL',
    'RUN_ATTEMPT_INVALID',
    'RUN_ID_MISMATCH',
    'RUN_REPOSITORY_MISMATCH',
    'RUN_STATUS_INVALID',
    'SCHEMA_INVALID',
    'SELF_PREDECESSOR',
    'SESSION_EXPIRED',
    'SMOKE_RESULT_INVALID',
    'SPEC_HASH_MISMATCH',
    'STALE_TASK_SNAPSHOT',
    'STATE_MISMATCH',
    'TASK_NOT_AUTHORIZED',
    'TRANSITION_FORBIDDEN',
    'TRUSTED_DIGEST_MISMATCH',
    'TRUST_DATA_UNAVAILABLE',
    'UNAPPROVED_SOURCE',
    'UNEXPECTED_APPROVAL',
    'UNKNOWN_PROTOCOL_VERSION',
    'UNTRUSTED_RUN_CONTEXT',
    'V1_APP_EXECUTION_FORBIDDEN',
})


class Rejected(ValueError):
    """Safe fixed reason code, never a transport exception or input payload."""

    def __init__(self, code):
        super().__init__(code if type(code) is str and code in REASON_CODES else 'TRUST_DATA_UNAVAILABLE')


def need(condition, code):
    if not condition:
        raise Rejected(code)


def native(value):
    need(type(value) in (dict, list, str, int, bool, type(None)), 'INVALID_JSON_TYPE')
    if type(value) is dict:
        for key, item in value.items():
            need(type(key) is str, 'INVALID_JSON_KEY')
            native(item)
    elif type(value) is list:
        for item in value:
            native(item)


def document(raw):
    try:
        value = load_json(raw)
        native(value)
        return value
    except Exception:
        raise Rejected('INVALID_JSON') from None


def validate_schema(value, name):
    native(value)
    definition = json.loads((Path(__file__).parents[1] / 'schemas' / name).read_text())
    validator = Draft202012Validator(definition, format_checker=FormatChecker())
    need(not list(validator.iter_errors(value)), 'SCHEMA_INVALID')
    return value


def instant(value):
    try:
        parsed = datetime.strptime(value, '%Y-%m-%dT%H:%M:%SZ').replace(tzinfo=timezone.utc)
        need(parsed.strftime('%Y-%m-%dT%H:%M:%SZ') == value, 'INVALID_TIME')
        return parsed
    except Exception:
        raise Rejected('INVALID_TIME') from None


def positive(value):
    return type(value) is int and value > 0


def version(value):
    need(type(value) is dict and type(value.get('schema_version')) is int,
         'PROTOCOL_VERSION_REQUIRED')
    need(value['schema_version'] in (1, 2), 'UNKNOWN_PROTOCOL_VERSION')
    return value['schema_version']


def app_task(value):
    need(version(value) == 2, 'V1_APP_EXECUTION_FORBIDDEN')
    return validate_schema(value, 'task-v2.json')
