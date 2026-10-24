# G2 — результаты A01–A32

Источник: матрица PR53 на `96d8535ec544fcd13e93335e3d0f096c966da0f8`.
Это результаты G2, не замена утверждённой архитектурной матрицы.
O = synthetic offline tests; N = source/configuration review; L = live GitHub evidence.
`passed` относится только к указанной части. `not_run` — тип проверки здесь не выполнялся;
`blocked` — необходим будущий ресурс/интеграция/разрешение. Failed на итоговом offline
наборе нет; trusted governance CI учитывается отдельно и не является identity test.

Сокращения tests: S=`test_v2_schema.py`, A=`test_v2_approval.py`,
C=`test_v2_controller.py`, V=`test_v2_compatibility.py` в `tests/agent_core/`.

| ID | O | N | L | Проверенное свидетельство / ограничение |
| --- | --- | --- | --- | --- |
| A01 | passed | not_run | blocked | C.downscope_and_revoke, A.exact_approval; только synthetic identity/run |
| A02 | passed | not_run | not_run | S.identity_types, A.approver_namespace; ID namespaces и Bot/User |
| A03 | passed | not_run | blocked | C.installation_identity_scope_status: wrong app_id |
| A04 | passed | not_run | blocked | C.installation_identity_scope_status: wrong install/account |
| A05 | passed | not_run | blocked | A.repository_and_head_repository, C.installation_identity_scope_status; перенос/переустановка моделируются несовпадением bindings |
| A06 | passed | blocked | not_run | C.downscope_and_revoke, lease_scope, incomplete_installation_at_each_read; реальный mint/pagination adapter отсутствует |
| A07 | passed | not_run | blocked | A.approver_namespace: чужой User, Bot, App ID |
| A08 | passed | passed | not_run | C.fake_execution_environment, V.no_network_even_when_credentials_present; нет PAT/OAuth/env credential adapter, физическое присутствие owner не доказано |
| A09 | passed | not_run | blocked | A.no_implicit_approval, event_workflow_and_title; comment/label/check не являются входами grant |
| A10 | passed | not_run | blocked | A.event_workflow_and_title, deleted_or_incomplete_evidence |
| A11 | passed | not_run | blocked | A.changed_spec_even_with_new_hash, protected_scope_even_with_consistent_hashes |
| A12 | passed | not_run | not_run | S.immutable_subject_and_identity, C.policy_registry_tamper, controller_metadata_tamper |
| A13 | passed | not_run | not_run | S.append_only, decision_history; A.immutable_history_prefix, wrong_history_position |
| A14 | passed | not_run | blocked | A.replay_requires_new_run, reused_nonce_new_position; C.second_action_revalidates |
| A15 | passed | not_run | blocked | C.snapshot_revoked, new_revision_revokes, second_action_revalidates |
| A16 | passed | not_run | not_run | C.consent_boundaries, clock_skew, token_does_not_extend_consent; S.request_window_and_position |
| A17 | passed | not_run | not_run | C.main_race: даже неизменённая задача при новом main запрещена |
| A18 | passed | not_run | blocked | C.fake_execution_environment, boolean_execution_fields, controller_metadata_tamper |
| A19 | passed | blocked | not_run | Source pins отклоняются в A/C; реальная изоляция checkout/cache/key-release не существует в G2 |
| A20 | passed | blocked | not_run | C.worker_credentials_not_returned, V.no_entrypoint_or_adapter; реальные job/workspace/cache границы требуют G3/G4 |
| A21 | passed | passed | not_run | C.fixed_operation, V.no_entrypoint_or_adapter; нет command/URL/path handler и новых dependencies; workflows не менялись |
| A22 | passed | blocked | not_run | C.exception_cleanup, cancellation_cleanup, revoke_failure_blocks, safe_audit_on_malformed_values; forced kill/platform cleanup не доказаны |
| A23 | passed | not_run | blocked | C.installation_identity_scope_status, incomplete_installation_at_each_read; permissions/selection growth denied |
| A24 | passed | not_run | blocked | C.revocation_during_check, fault_at_every_read; suspension/uninstall/refusal synthetic |
| A25 | passed | not_run | blocked | C.lease_expired, lease_expires_during_verification, consent_expires_during_check |
| A26 | passed | not_run | not_run | C.fault_at_every_read: timeout/401/403/404/429/500/bad JSON exceptions на каждом read; S.strict_json; неполный listing rejected. Реальный HTTP/pagination/parser adapter не проверен |
| A27 | passed | not_run | not_run | V.v1_unchanged, v1_grant_not_transferred, v1_api_not_extended, v2_rejected_by_v1 |
| A28 | passed | passed | not_run | C.fixed_operation; только фиксированный smoke, без shell/PR/implementation handler |
| A29 | passed | passed | blocked | A.protected_scope_even_with_consistent_hashes; нет mutation adapter/credentials. Реальных API403/mutation попыток не было |
| A30 | not_run | blocked | not_run | Environment/self-review недоступны для G2 проверки; D02 без fallback, будущий G3 STOP при несовместимости |
| A31 | passed | not_run | not_run | A.deleted_or_incomplete_evidence, S.versions, C.missing_records; durable audit service отсутствует, архив не становится authority |
| A32 | passed | blocked | not_run | C.aba_is_not_claimed_detectable; ограничения TOCTOU/in-flight документированы, write локально запрещён. Branch protection/required checks не включались |

Для A19/A20/A22 passed O означает проверку модели/отсутствия каналов в исходниках,
а не доказанную изоляцию реального runner. Для A26 fault statuses — synthetic errors,
а `complete` — normalized contract, не доказательство обхода реальных API страниц.
Live набор целиком остаётся без выполнения; разрешения G3–G6 не получены.
