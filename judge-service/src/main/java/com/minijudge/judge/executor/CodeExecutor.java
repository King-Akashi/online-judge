package com.minijudge.judge.executor;

import java.io.IOException;

public interface CodeExecutor {

    CodeExecutionSession createSession(String sourceCode, int memoryLimitMb) throws IOException;
}
