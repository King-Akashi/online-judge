CREATE TABLE judge_jobs (
    submission_id UUID PRIMARY KEY,
    request_event_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    verdict VARCHAR(8),
    execution_time_ms BIGINT,
    memory_used_bytes BIGINT,
    passed_tests INTEGER,
    total_tests INTEGER,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE result_outbox (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    submission_id UUID NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000)
);

CREATE INDEX ix_result_outbox_pending
    ON result_outbox(next_attempt_at, created_at)
    WHERE published_at IS NULL;
