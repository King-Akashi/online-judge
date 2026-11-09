# Reliability and Security Notes

- The Submission Service writes the submission and `SubmissionRequested`
  outbox row in one database transaction. A publisher sends persistent messages
  to durable RabbitMQ queues and marks an outbox row published only after a
  broker confirmation. A crash between confirmation and the database update can
  cause redelivery; consumers use event IDs and terminal state to be idempotent.
- Judge results use a second transactional outbox. The request queue and result
  queues retry transient processing failures up to three attempts, then route
  rejected messages to durable dead-letter queues.
- Each Judge Service instance processes one submission at a time
  (`concurrency=1`, `prefetch=1`). Each individual execution also has explicit
  CPU, memory, PID, output-size, and wall-clock limits.
- New `RUNNING` judge jobs retain their original request. If one remains stale
  for five minutes, the recovery scheduler republishes it; configure
  `STALE_JOB_MINUTES` above the maximum expected evaluation duration. Legacy
  jobs without a persisted request cannot be replayed and are logged and skipped
  so they do not block recovery of other jobs. The submission and judge services
  should be monitored through their actuator health endpoints and dead-letter
  queues.
- Hidden inputs are excluded from all public Problem Service DTOs. The internal
  judge API is disabled until `MINIJUDGE_INTERNAL_API_KEY` is configured.
- The Compose setup is a developer environment, not a production security
  boundary. It uses local credentials and gives the trusted Judge Service
  access to Docker for container creation. Do not mount that socket in a runner
  container; for public workloads, use a dedicated isolated Docker host or a
  managed sandbox service.

Recovery is intentionally bounded and at-least-once, not a distributed
exactly-once guarantee. An operator should inspect the dead-letter queues and
actuator health before replaying messages.
