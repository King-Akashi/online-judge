CREATE TABLE submissions (
    id UUID PRIMARY KEY,
    problem_id UUID NOT NULL,
    language VARCHAR(16) NOT NULL,
    source_code TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    verdict VARCHAR(8),
    execution_time_ms BIGINT,
    memory_used_bytes BIGINT,
    passed_tests INTEGER,
    total_tests INTEGER,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_submissions_created_at ON submissions(created_at DESC);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000)
);

CREATE INDEX ix_outbox_events_pending
    ON outbox_events(next_attempt_at, created_at)
    WHERE published_at IS NULL;

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    received_at TIMESTAMPTZ NOT NULL
);
