CREATE TABLE problems (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL UNIQUE,
    difficulty VARCHAR(16) NOT NULL,
    language VARCHAR(16) NOT NULL,
    statement TEXT NOT NULL,
    constraints_text TEXT NOT NULL,
    starter_code TEXT NOT NULL,
    time_limit_ms INTEGER NOT NULL CHECK (time_limit_ms > 0),
    memory_limit_mb INTEGER NOT NULL CHECK (memory_limit_mb > 0),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE test_cases (
    id UUID PRIMARY KEY,
    problem_id UUID NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    input TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    sample BOOLEAN NOT NULL,
    order_index INTEGER NOT NULL CHECK (order_index >= 0),
    CONSTRAINT uq_test_case_problem_order UNIQUE (problem_id, order_index)
);

CREATE INDEX ix_test_cases_problem_id ON test_cases(problem_id);
