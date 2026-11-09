package com.minijudge.judge.executor;

import java.io.IOException;

public interface CodeExecutionSession extends AutoCloseable {

    CodeExecutionResult compile();

    CodeExecutionResult execute(String input, int timeLimitMs);

    @Override
    void close() throws IOException;
}
