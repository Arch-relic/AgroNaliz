"""Pure simulated result; no network/client/environment imports."""
from .canonical import require
from .contracts import IDENTITY


def verify_link(projection, reference, spec, task_digest, run_id, response_digest, diff_digest, result_digest):
    require(spec['publishAllowed'] is False, 'REAL_PUBLISH_FORBIDDEN')
    require(type(reference) is tuple and len(reference) == 3, 'INTEGRITY_UNVERIFIED')
    validation = projection.validations.get(run_id)
    worker = projection.worker_results.get(run_id)
    require(validation is not None and worker is not None, 'INTEGRITY_UNVERIFIED')
    require(reference == (validation['logId'], validation['sequenceNo'], validation['eventHash']), 'INTEGRITY_FAILED')
    require(validation['actor'] == 'validator' and validation['eventType'] == 'RESULT'
            and validation['payload']['operationKind'] == 'VALIDATOR', 'INTEGRITY_FAILED')
    receipt = validation['payload']['validationReceipt']
    require(receipt['validationStatus'] == 'PASSED', 'INVALID_DIFF')
    expected = {k: spec[k] for k in IDENTITY if k in spec}
    expected.update(taskDigest=task_digest, runId=run_id)
    require(all(receipt[k] == v for k, v in expected.items()), 'INTEGRITY_FAILED')
    require(receipt['workerResponseDigest'] == worker['payload']['workerResponseDigest'] == response_digest
            and receipt['diffDigest'] == diff_digest and receipt['resultSnapshotDigest'] == result_digest, 'HASH_MISMATCH')
    require(receipt['operationId'] != worker['operationId'], 'INTEGRITY_FAILED')
    require(projection.runs[run_id]['state'] == 'VALIDATING', 'INTEGRITY_FAILED')
    return receipt


def simulated_payload(kind, run_id, validation_event, receipt):
    return dict(kind=kind, runId=run_id, validationEventSequenceNo=validation_event['sequenceNo'],
                diffDigest=receipt['diffDigest'], resultSnapshotDigest=receipt['resultSnapshotDigest'],
                simulated=True, externalEffects=0)
