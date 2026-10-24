"""Offline verification of normalized evidence, not a GitHub network adapter."""
from dataclasses import dataclass
import re
from agent_core.core import digest
from agent_core.v2.model import need, positive, validate_schema


@dataclass(frozen=True)
class OfflineAnchors:
    """Test-supplied reviewed pins; never accepted as live task/dispatch authority."""
    repository_id: int
    repository_name: str
    owner_id: int
    registry_hash: str
    policy_hash: str


def bindings(registry, policy, anchors):
    validate_schema(registry, 'registry-v2.json')
    validate_schema(policy, 'policy-v2.json')
    need(digest(registry) == anchors.registry_hash and digest(policy) == anchors.policy_hash,
         'TRUSTED_DIGEST_MISMATCH')
    need(registry['policy_hash'] == anchors.policy_hash
         and registry['owner_id'] == anchors.owner_id
         and registry['repository_name'] == anchors.repository_name
         and registry['executor']['repository_id'] == anchors.repository_id
         and registry['executor']['policy_version'] == policy['version'], 'REGISTRY_BINDING_MISMATCH')


def verify_run(run, workflow, anchors, *, approval):
    need(positive(run['id']) and type(run['run_attempt']) is int and run['run_attempt'] == 1,
         'RUN_ATTEMPT_INVALID')
    for key in ('repository', 'head_repository'):
        need(type(run[key]['id']) is int and run[key]['id'] == anchors.repository_id
             and run[key]['full_name'] == anchors.repository_name, 'RUN_REPOSITORY_MISMATCH')
    for key in ('actor', 'triggering_actor'):
        need(type(run[key]['id']) is int and run[key]['id'] == anchors.owner_id
             and run[key]['type'] == 'User', 'OWNER_IDENTITY_REQUIRED')
    need(run['event'] == 'workflow_dispatch' and run['head_branch'] == 'main'
         and run['path'] == workflow['path'] and type(run['workflow_id']) is int
         and run['workflow_id'] == workflow['id'], 'UNTRUSTED_RUN_CONTEXT')
    need(run['source_sha'] == workflow['source_sha'] and run['source_hash'] == workflow['source_hash']
         and re.fullmatch('[a-f0-9]{40}', run['head_sha']), 'UNAPPROVED_SOURCE')
    need(run['status'] == ('completed' if approval else 'in_progress')
         and run['conclusion'] == ('success' if approval else None), 'RUN_STATUS_INVALID')


def installation(value, registry):
    subject = registry['executor']
    for key in ('app_id', 'installation_id', 'installation_account_id'):
        need(positive(value[key]) and value[key] == subject[key], 'INSTALLATION_IDENTITY_MISMATCH')
    need(value['suspended'] is False and value['active'] is True, 'INSTALLATION_UNAVAILABLE')
    need(value['permissions'] == registry['permissions']
         and value['repository_selection'] == 'selected'
         and value['repository_ids'] == [subject['repository_id']]
         and value['complete'] is True, 'INSTALLATION_SCOPE_MISMATCH')
