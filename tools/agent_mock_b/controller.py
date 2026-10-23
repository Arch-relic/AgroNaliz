"""Synchronous single-writer offline controller. All permissions are synthetic."""
import copy

from . import PROTOCOL
from .approval_store import Trust
from .budget import action_limit, reservation
from .canonical import Refusal, digest, loads, require
from .contracts import fields, specification, uuid, response, IDENTITY, integer, hexstr
from .snapshot import Snapshot
from .diff_validator import validate
from .evidence import PROTECT
from .fake_worker import FakeWorker
from .recovery import replay
from .simulated_publisher import verify_link, simulated_payload
from .state_machine import TERMINAL, BLOCKED


class Controller:
    externalCalls = 0
    publishAllowed = False
    workerKind = 'mock'

    def __init__(self, trust, approvals, repositories, evidence, worker, clock, hook=lambda _: None):
        require(type(worker) is FakeWorker, 'REAL_PUBLISH_FORBIDDEN')
        self.trust, self.approvals, self.repositories = trust, approvals, repositories
        self.evidence, self.worker, self.clock, self.hook = evidence, worker, clock, hook
        self.publisherCalls = 0
        self.localStatus = None

    def _fresh(self, spec, task_digest, envelope):
        record = self.approvals.check(spec, task_digest, envelope, self.trust, self.clock.now())
        snapshot = self.repositories.read(spec, self.trust.expectedRepository)
        self.evidence.confirmed()
        return record, snapshot

    def _append(self, kind, payload, **kw):
        return self.evidence.append(kind, payload, self.clock.now(), hook=self.hook, **kw)

    def _checked(self, task_digest, envelope):
        self._append('APPROVAL_CHECKED', dict(kind='APPROVAL_CHECKED', approvalId=envelope['approvalId'],
            taskDigest=task_digest, ownerId=self.trust.expectedOwnerId, checkedAt=self.clock.now(), reason='FRESH'))

    def _guard(self, spec, task_digest, envelope, run_id, started):
        _, snapshot = self._fresh(spec, task_digest, envelope)
        require(self.evidence.writable and self.evidence.checkpoint.writable, 'STORE_UNAVAILABLE')
        projection = replay(self.evidence)
        run = projection.runs.get(run_id)
        require(not run or run['pair'] == (task_digest, envelope['approvalId']), 'INVALID_APPROVAL')
        require(projection.tasks.get((task_digest, envelope['approvalId'])) not in
                ('DENIED', 'REVOKED', 'EXPIRED', 'CLOSED'), 'INVALID_APPROVAL')
        require(not run or run['state'] not in TERMINAL + BLOCKED, 'RECOVERY_REQUIRED')
        elapsed = self.clock.monotonic_ms() - started
        require(0 <= elapsed <= spec['limits']['maxWallDurationMs'], 'LIMIT_EXCEEDED')
        return projection, snapshot

    def _intent(self, kind, op_id, input_digest, spec, task_digest, envelope, run_id, started):
        projection, _ = self._guard(spec, task_digest, envelope, run_id, started)
        run = projection.runs.get(run_id)
        require(op_id not in projection.receipts and op_id not in projection.pending, 'IDEMPOTENCY_CONFLICT')
        action_limit(spec['limits'], run['operations'] if run else 0, self.clock.monotonic_ms() - started)
        budget = projection.budgets.get(task_digest, {'runs': 0, 'bytes': 0})
        if kind == 'WORKER':
            require(run is None, 'IDEMPOTENCY_CONFLICT')
            reserved, remaining = reservation(spec['limits'], budget['runs'], budget['bytes'])
        else:
            reserved, remaining = 0, max(0, spec['limits']['maxTotalOutputBytesPerTask'] - budget['bytes'])
        pair = (task_digest, envelope['approvalId'])
        old_task = projection.tasks[pair]
        payload = dict(kind='INTENT', operationKind=kind, operationId=op_id, inputDigest=input_digest,
            taskDigest=task_digest, approvalId=envelope['approvalId'], runId=run_id,
            fromTaskState=old_task, toTaskState='ACTIVE', fromRunState=run['state'] if run else 'CREATED',
            toRunState='INTENT_RECORDED' if kind == 'WORKER' else 'VALIDATING',
            reservedRunSlot=1 if kind == 'WORKER' else 0, reservedOutputBytes=reserved,
            remainingTaskOutputBudgetBefore=remaining, baseSha=spec['baseSha'], snapshotDigest=spec['snapshotDigest'])
        self._append('INTENT', payload, run_id=run_id, operation_id=op_id, input_digest=input_digest,
            before_checkpoint=lambda: self._fresh_for_checkpoint(spec, task_digest, envelope, started))
        return reserved

    def _result(self, kind, op_id, input_digest, spec, task_digest, envelope, run_id, started,
                actual=0, worker_digest=None, validation_receipt=None, outcome='OK'):
        projection, _ = self._guard(spec, task_digest, envelope, run_id, started)
        run = projection.runs[run_id]
        final = kind == 'SIMULATED_PUBLISH'
        receipt = dict(taskId=spec['taskId'], runId=run_id, operationId=op_id,
                       inputDigest=input_digest, phase=kind, outcome=outcome)
        payload = dict(kind='RESULT', operationKind=kind, operationId=op_id, inputDigest=input_digest,
            runId=run_id, fromTaskState=projection.tasks[run['pair']], toTaskState='REVIEWABLE' if final else 'ACTIVE',
            fromRunState=run['state'], toRunState='FAILED' if outcome != 'OK' else
            ('RESULT_RECEIVED' if kind == 'WORKER' else 'READY_FOR_OWNER_REVIEW' if final else 'VALIDATING'),
            actualOutputBytes=actual, receipt=receipt, validationReceipt=validation_receipt,
            workerResponseDigest=worker_digest)
        # A revocation between append and checkpoint leaves a blocking unconfirmed tail.
        return self._append('RESULT', payload, run_id=run_id, operation_id=op_id, input_digest=input_digest,
            actor={'WORKER': 'worker', 'VALIDATOR': 'validator', 'SIMULATED_PUBLISH': 'simulated-publisher'}[kind],
            outcome=outcome, before_checkpoint=lambda: self._fresh_for_checkpoint(spec, task_digest, envelope, started))

    def _fresh_for_checkpoint(self, spec, task_digest, envelope, started):
        # Do not call confirmed(): the current append intentionally has an unconfirmed tail.
        self.approvals.check(spec, task_digest, envelope, self.trust, self.clock.now())
        self.repositories.read(spec, self.trust.expectedRepository)
        elapsed = self.clock.monotonic_ms() - started
        require(0 <= elapsed <= spec['limits']['maxWallDurationMs'], 'LIMIT_EXCEEDED')

    def _authorization_refusal(self, task_digest, envelope, code):
        if code not in ('APPROVAL_REVOKED', 'APPROVAL_EXPIRED', 'INVALID_APPROVAL'):
            return
        try:
            projection = replay(self.evidence)
            pair = (task_digest, envelope['approvalId'])
            old = projection.tasks.get(pair, 'PROPOSED')
            if code == 'INVALID_APPROVAL' and old != 'PROPOSED':
                return
            if old in ('DENIED', 'REVOKED', 'EXPIRED', 'CLOSED'):
                return
            kind = {'APPROVAL_REVOKED': 'REVOKED', 'APPROVAL_EXPIRED': 'EXPIRED',
                    'INVALID_APPROVAL': 'APPROVAL_DENIED'}[code]
            # An initial revoked/expired request never had an approved lifecycle.
            if old == 'PROPOSED' and kind != 'APPROVAL_DENIED':
                kind = 'APPROVAL_DENIED'
            self._append(kind, dict(kind=kind, approvalId=envelope['approvalId'], taskDigest=task_digest,
                ownerId=self.trust.expectedOwnerId, checkedAt=self.clock.now(), reason=code), outcome='REJECTED')
        except (Refusal, OSError, KeyError, AttributeError):
            self.localStatus = 'STOP_REQUESTED_UNVERIFIED'

    def stop(self, run_id, reason):
        """Best effort cancel does not require approval/checkpoint availability."""
        confirmed = self.worker.cancel(run_id)
        try:
            projection = replay(self.evidence)
            if run_id not in projection.runs or projection.runs[run_id]['state'] in TERMINAL:
                return
            kind = 'CANCEL_CONFIRMED' if confirmed else 'STOP_REQUESTED'
            self._append(kind, dict(kind=kind, runId=run_id, operationId=None, reason=reason,
                                   confirmedStop=confirmed), run_id=run_id,
                         outcome='OK' if confirmed else 'UNKNOWN')
            self.localStatus = 'CANCELLED' if confirmed else 'CANCELLATION_PENDING'
        except (Refusal, OSError):
            self.localStatus = 'STOP_REQUESTED_UNVERIFIED'

    def execute(self, spec, envelope, run_id, operation_ids, worker_context=None):
        """No env/file/credential capability is accepted by this synthetic model."""
        spec = specification(spec)
        require(spec['taskId'] == self.evidence.task_id, 'INVALID_SCHEMA')
        uuid(run_id)
        fields(operation_ids, ('WORKER', 'VALIDATOR', 'SIMULATED_PUBLISH'))
        for op in operation_ids.values():
            uuid(op)
        require(len(set(operation_ids.values())) == 3, 'INVALID_SCHEMA')
        if worker_context is not None:
            fields(worker_context, ('payload', 'env', 'accessibleFiles'))
            require(worker_context == {'payload': {}, 'env': {}, 'accessibleFiles': {}}, 'INVALID_SCHEMA')
        task_digest = digest('spec', spec)
        worker_input = digest('operation', {'spec': spec, 'envelope': envelope, 'runId': run_id,
                                            'operationIds': operation_ids})
        with self.evidence.lock:
            try:
                projection = replay(self.evidence)
                op = operation_ids['WORKER']
                if op in projection.receipts:
                    historical = projection.receipts[op]
                    require(historical['inputDigest'] == worker_input, 'IDEMPOTENCY_CONFLICT')
                    return {'historicalReceipt': copy.deepcopy(historical)}
                self.approvals.bind_log(task_digest, self.evidence)
                require(op not in projection.pending, 'RECOVERY_REQUIRED')
                require(not any(projection.runs[p['runId']]['state'] not in ('FAILED', 'CANCELLED')
                                for p in projection.pending.values()), 'RECOVERY_REQUIRED')
                _, snapshot = self._fresh(spec, task_digest, envelope)
                self._checked(task_digest, envelope)
                started = self.clock.monotonic_ms()
                reserved = self._intent('WORKER', op, worker_input, spec, task_digest, envelope, run_id, started)
                started = self.clock.monotonic_ms()
                self.hook('WORKER:before-action')
                self._guard(spec, task_digest, envelope, run_id, started)
                request = {k: spec[k] for k in IDENTITY if k in spec}
                request.update(taskDigest=task_digest, runId=run_id, operationId=op)
                raw = bytearray()
                for chunk in self.worker.stream(request, worker_input):
                    require(type(chunk) is bytes, 'INVALID_SCHEMA')
                    require(len(raw) + len(chunk) <= reserved, 'LIMIT_EXCEEDED')
                    raw.extend(chunk)
                    self._guard(spec, task_digest, envelope, run_id, started)
                self.hook('WORKER:action')
                self._guard(spec, task_digest, envelope, run_id, started)
                snapshot.verify()
                obj = loads(bytes(raw)); response(obj)
                require(all(obj[k] == v for k, v in request.items()), 'INVALID_DIFF')
                worker_digest = digest('worker-response', obj)
                self._result('WORKER', op, worker_input, spec, task_digest, envelope, run_id, started,
                             actual=len(raw), worker_digest=worker_digest)
                # Only the checkpointed RESULT supplies the digest to the validator.
                worker_event = replay(self.evidence).worker_results[run_id]
                require(worker_event['payload']['workerResponseDigest'] == worker_digest, 'HASH_MISMATCH')
                validator_op = operation_ids['VALIDATOR']
                validator_input = digest('operation', {'runId': run_id, 'workerResponseDigest': worker_digest})
                self._intent('VALIDATOR', validator_op, validator_input, spec, task_digest, envelope, run_id, started)
                self.hook('VALIDATOR:before-action')
                _, snapshot = self._guard(spec, task_digest, envelope, run_id, started)
                try:
                    vr, result = validate(spec, task_digest, snapshot, obj, worker_event['payload']['workerResponseDigest'],
                                          run_id, op, validator_op)
                except Refusal:
                    vr = {k: request[k] for k in IDENTITY}
                    vr.update(operationId=validator_op, workerResponseDigest=worker_digest,
                        diffDigest=digest('diff', {'diffOps': obj['diffOps']}), resultSnapshotDigest=snapshot.digest,
                        validationStatus='REJECTED', validatorVersion=PROTOCOL)
                    self._result('VALIDATOR', validator_op, validator_input, spec, task_digest, envelope,
                                 run_id, started, validation_receipt=vr, outcome='REJECTED')
                    raise
                self.hook('VALIDATOR:action')
                event = self._result('VALIDATOR', validator_op, validator_input, spec, task_digest, envelope,
                                     run_id, started, validation_receipt=vr)
                self.hook('VALIDATOR:confirmed')
                reference = (event['logId'], event['sequenceNo'], event['eventHash'])
                return self.publish_simulated(spec, envelope, run_id, operation_ids['SIMULATED_PUBLISH'], reference,
                                              obj, result, started)
            except Refusal as error:
                self._authorization_refusal(task_digest, envelope, error.code)
                # No fictional run is created for an error before WORKER INTENT.
                self.stop(run_id, error.code)
                raise
            except OSError:
                self.stop(run_id, 'STORE_UNAVAILABLE')
                raise Refusal('STORE_UNAVAILABLE') from None

    def publish_simulated(self, spec, envelope, run_id, operation_id, reference, obj, result, started):
        spec = specification(spec)
        uuid(run_id); uuid(operation_id); integer(started)
        require(type(reference) is tuple and len(reference) == 3, 'INTEGRITY_UNVERIFIED')
        uuid(reference[0]); integer(reference[1], 1); hexstr(reference[2])
        response(obj)
        require(type(result) is Snapshot, 'INVALID_SCHEMA')
        task_digest = digest('spec', spec)
        with self.evidence.lock:
            projection = replay(self.evidence)
            # Historical receipt retrieval is read-only, including after revocation.
            input_digest = digest('operation', {'runId': run_id, 'validationEvent': list(reference),
                'workerResponseDigest': digest('worker-response', obj),
                'diffDigest': digest('diff', {'diffOps': obj.get('diffOps', [])}), 'resultSnapshotDigest': result.digest})
            if operation_id in projection.receipts:
                historical = projection.receipts[operation_id]
                require(historical['inputDigest'] == input_digest, 'IDEMPOTENCY_CONFLICT')
                return {'historicalReceipt': copy.deepcopy(historical)}
            projection, snapshot = self._guard(spec, task_digest, envelope, run_id, started)
            require(not projection.pending, 'RECOVERY_REQUIRED')
            run = projection.runs.get(run_id)
            require(run is not None, 'INTEGRITY_UNVERIFIED')
            # Independently reapply the diff instead of trusting caller-provided bytes.
            worker_event = projection.worker_results.get(run_id)
            require(worker_event is not None, 'INTEGRITY_UNVERIFIED')
            validator_event = projection.validations.get(run_id)
            require(validator_event is not None, 'INTEGRITY_UNVERIFIED')
            vr, rebuilt = validate(spec, task_digest, snapshot, obj, worker_event['payload']['workerResponseDigest'],
                run_id, worker_event['operationId'], validator_event['operationId'])
            result.verify()
            require(result.digest == rebuilt.digest, 'HASH_MISMATCH')
            receipt = verify_link(projection, reference, spec, task_digest, run_id,
                                  digest('worker-response', obj), vr['diffDigest'], rebuilt.digest)
            require(operation_id not in (worker_event['operationId'], validator_event['operationId']), 'INVALID_SCHEMA')
            uuid(operation_id)
            input_digest = digest('operation', {'runId': run_id, 'validationEvent': list(reference),
                                                'workerResponseDigest': digest('worker-response', obj),
                                                'diffDigest': vr['diffDigest'], 'resultSnapshotDigest': rebuilt.digest})
            self._intent('SIMULATED_PUBLISH', operation_id, input_digest, spec, task_digest, envelope, run_id, started)
            self._guard(spec, task_digest, envelope, run_id, started)
            self._append('SIMULATED_PUBLISH_REQUESTED', simulated_payload('SIMULATED_PUBLISH_REQUESTED', run_id, validator_event, receipt),
                         run_id=run_id, operation_id=operation_id, input_digest=input_digest, actor='simulated-publisher',
                         before_checkpoint=lambda: self._fresh_for_checkpoint(spec, task_digest, envelope, started))
            self.hook('SIMULATED_PUBLISH:before-action')
            self._guard(spec, task_digest, envelope, run_id, started)
            self.publisherCalls += 1
            self._append('SIMULATED_PUBLISHED', simulated_payload('SIMULATED_PUBLISHED', run_id, validator_event, receipt),
                         run_id=run_id, operation_id=operation_id, input_digest=input_digest, actor='simulated-publisher',
                         before_checkpoint=lambda: self._fresh_for_checkpoint(spec, task_digest, envelope, started))
            self.hook('SIMULATED_PUBLISH:action')
            event = self._result('SIMULATED_PUBLISH', operation_id, input_digest, spec, task_digest, envelope, run_id, started)
            return {'receipt': event['payload']['receipt'], 'resultSnapshot': rebuilt,
                    'projection': replay(self.evidence), 'validationReference': reference}
