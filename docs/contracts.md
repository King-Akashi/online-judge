# MiniJudge Service and Event Contracts

This document is the shared contract baseline for the Problem, Submission, and
Judge Services. Public HTTP APIs use JSON and the `/api/v1` prefix. Identifiers
are UUIDs. Timestamps, when added to API responses, use ISO-8601 UTC.

## Ownership and service APIs

| Service | Owns | Contract |
| --- | --- | --- |
| Problem Service | Problems and test cases | `POST /api/v1/problems` creates a problem; `GET /api/v1/problems` lists public summaries; `GET /api/v1/problems/{problemId}` returns the statement, constraints, starter code, and sample tests without hidden test inputs. Internal `GET /internal/v1/problems/{problemId}/test-cases` returns the complete suite and requires `X-Internal-Token`. |
| Submission Service | Source code and submission lifecycle | `POST /api/v1/submissions` accepts `{ "problemId": "<uuid>", "language": "JAVA", "sourceCode": "..." }`; `GET /api/v1/submissions/{submissionId}` returns status and, when available, verdict and metrics; `GET /api/v1/submissions` returns the latest 100 submissions across the service. |
| Judge Service | Evaluation work, not problem or submission records | Consumes `SubmissionRequested`; obtains test cases only through the Problem Service internal API; publishes `SubmissionEvaluated`. |

No service reads or writes another service's database. The internal test-case
API is not part of the public problem response and must not expose hidden inputs
to CLI users. Set `MINIJUDGE_INTERNAL_API_KEY` on the Problem Service and pass
the same value in `X-Internal-Token` from the Judge Service. The internal route
is unavailable when no key is configured.

## Submission lifecycle

`PENDING -> QUEUED -> RUNNING -> COMPLETED`

`FAILED` is reserved for infrastructure failures that prevent evaluation from
producing a verdict. The verdict (`AC`, `WA`, `CE`, `TLE`, or `RE`) is separate
from lifecycle status and is set only when status becomes `COMPLETED`. Legal
transitions are forward-only; duplicate events must not change an already
terminal submission.

## Events

Events are versioned JSON messages. `eventId` is a UUID used for idempotency;
`submissionId` is the correlation key. The Judge Service publishes
`SubmissionStarted` when it claims an event so `RUNNING` is observable.

### `SubmissionStarted` (v1)

```json
{
  "eventId": "uuid",
  "submissionId": "uuid",
  "occurredAt": "2025-01-01T00:00:00Z",
  "schemaVersion": 1
}
```

### `SubmissionRequested` (v1)

```json
{
  "eventId": "uuid",
  "submissionId": "uuid",
  "problemId": "uuid",
  "language": "JAVA",
  "sourceCode": "public class Main { ... }",
  "occurredAt": "2025-01-01T00:00:00Z",
  "schemaVersion": 1
}
```

The Submission Service persists the submission before publishing this event.
Publishing will use a transactional outbox when implemented.

### `SubmissionEvaluated` (v1)

```json
{
  "eventId": "uuid",
  "submissionId": "uuid",
  "verdict": "AC",
  "executionTimeMs": 12,
  "memoryUsedBytes": 1048576,
  "passedTests": 3,
  "totalTests": 3,
  "occurredAt": "2025-01-01T00:00:01Z",
  "schemaVersion": 1
}
```

`executionTimeMs`, `memoryUsedBytes`, `passedTests`, and `totalTests` are
non-negative; passed tests cannot exceed total tests. The Submission Service
correlates the result by `submissionId` and handles duplicate deliveries
idempotently.

## Error response

All API errors use the same shape:

```json
{
  "type": "VALIDATION_ERROR",
  "title": "Request validation failed",
  "status": 400,
  "detail": "language must be JAVA",
  "instance": "/api/v1/submissions"
}
```

`type` is a stable machine-readable code; `title` and `detail` are safe for
clients to display. Internal stack traces and hidden test data are never
returned.
