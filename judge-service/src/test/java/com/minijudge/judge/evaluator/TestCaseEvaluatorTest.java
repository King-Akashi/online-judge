package com.minijudge.judge.evaluator;

import com.minijudge.contracts.Verdict;
import com.minijudge.judge.client.ProblemTestCase;
import com.minijudge.judge.executor.CodeExecutionResult;
import com.minijudge.judge.executor.CodeExecutionSession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestCaseEvaluatorTest {

    @Test
    void normalizesLineEndingsAndTrailingWhitespaceOnly() {
        CodeExecutionSession session = mock(CodeExecutionSession.class);
        when(session.execute("input", 1000)).thenReturn(
                new CodeExecutionResult(0, "value  \r\n", "", 4, 1024, false, false));
        ProblemTestCase testCase = new ProblemTestCase(
                UUID.randomUUID(), "input", "value\n", true, 0);

        JudgeOutcome outcome = new TestCaseEvaluator().evaluate(session, List.of(testCase), 1000);

        assertThat(outcome.verdict()).isEqualTo(Verdict.AC);
        assertThat(outcome.passedTests()).isEqualTo(1);
    }

    @Test
    void preservesMeaningfulInternalWhitespace() {
        CodeExecutionSession session = mock(CodeExecutionSession.class);
        when(session.execute("input", 1000)).thenReturn(
                new CodeExecutionResult(0, "a  b\n", "", 4, 1024, false, false));
        ProblemTestCase testCase = new ProblemTestCase(
                UUID.randomUUID(), "input", "a b\n", true, 0);

        JudgeOutcome outcome = new TestCaseEvaluator().evaluate(session, List.of(testCase), 1000);

        assertThat(outcome.verdict()).isEqualTo(Verdict.WA);
    }
}
