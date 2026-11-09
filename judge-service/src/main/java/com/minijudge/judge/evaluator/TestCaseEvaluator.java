package com.minijudge.judge.evaluator;

import com.minijudge.contracts.Verdict;
import com.minijudge.judge.client.ProblemTestCase;
import com.minijudge.judge.executor.CodeExecutionResult;
import com.minijudge.judge.executor.CodeExecutionSession;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TestCaseEvaluator {

    public JudgeOutcome evaluate(
            CodeExecutionSession session,
            List<ProblemTestCase> testCases,
            int timeLimitMs) {
        int passed = 0;
        long elapsed = 0;
        long memoryUsed = 0;
        Verdict failureVerdict = null;

        for (ProblemTestCase testCase : testCases) {
            CodeExecutionResult result = session.execute(testCase.input(), timeLimitMs);
            elapsed += result.executionTimeMs();
            memoryUsed = Math.max(memoryUsed, result.memoryUsedBytes());

            if (result.timedOut()) {
                failureVerdict = Verdict.TLE;
                break;
            }
            if (result.outputLimitExceeded() || result.exitCode() != 0) {
                failureVerdict = Verdict.RE;
                break;
            }
            if (normalize(result.standardOutput()).equals(normalize(testCase.expectedOutput()))) {
                passed++;
            } else {
                failureVerdict = Verdict.WA;
            }
        }

        Verdict verdict = failureVerdict == null ? Verdict.AC : failureVerdict;
        return new JudgeOutcome(verdict, elapsed, memoryUsed, passed, testCases.size());
    }

    static String normalize(String value) {
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n')
                .replaceAll("[ \\t]+(?=\\n|$)", "");
        return normalized.replaceAll("\\n+$", "");
    }
}
