package com.minijudge.judge.service;

import com.minijudge.contracts.Language;
import com.minijudge.contracts.SubmissionEvaluated;
import com.minijudge.contracts.SubmissionRequested;
import com.minijudge.contracts.SubmissionStarted;
import com.minijudge.contracts.Verdict;
import com.minijudge.judge.client.ProblemClient;
import com.minijudge.judge.client.ProblemDetailsResponse;
import com.minijudge.judge.client.ProblemTestCase;
import com.minijudge.judge.evaluator.JudgeOutcome;
import com.minijudge.judge.evaluator.TestCaseEvaluator;
import com.minijudge.judge.executor.CodeExecutionResult;
import com.minijudge.judge.executor.CodeExecutionSession;
import com.minijudge.judge.executor.CodeExecutor;
import com.minijudge.judge.messaging.JudgeEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JudgeOrchestrator {

    private final JudgeJobStore judgeJobStore;
    private final ProblemClient problemClient;
    private final CodeExecutor codeExecutor;
    private final TestCaseEvaluator testCaseEvaluator;
    private final JudgeEventPublisher eventPublisher;

    public JudgeOrchestrator(
            JudgeJobStore judgeJobStore,
            ProblemClient problemClient,
            CodeExecutor codeExecutor,
            TestCaseEvaluator testCaseEvaluator,
            JudgeEventPublisher eventPublisher) {
        this.judgeJobStore = judgeJobStore;
        this.problemClient = problemClient;
        this.codeExecutor = codeExecutor;
        this.testCaseEvaluator = testCaseEvaluator;
        this.eventPublisher = eventPublisher;
    }

    public void process(SubmissionRequested request) {
        if (request.language() != Language.JAVA) {
            throw new IllegalArgumentException("Only Java submissions are supported");
        }
        if (!judgeJobStore.begin(request)) {
            return;
        }

        eventPublisher.publishStarted(new SubmissionStarted(
                UUID.randomUUID(), request.submissionId(), Instant.now(), 1));

        ProblemDetailsResponse problem = problemClient.getProblem(request.problemId());
        List<ProblemTestCase> testCases = problemClient.getTestCases(request.problemId());
        if (testCases.isEmpty()) {
            throw new IllegalStateException("Problem has no test cases");
        }

        JudgeOutcome outcome;
        try (CodeExecutionSession session =
                     codeExecutor.createSession(request.sourceCode(), problem.memoryLimitMb())) {
            CodeExecutionResult compilation = session.compile();
            if (compilation.timedOut()) {
                outcome = new JudgeOutcome(
                        Verdict.TLE, compilation.executionTimeMs(), 0, 0, testCases.size());
            } else if (!compilation.succeeded()) {
                outcome = new JudgeOutcome(
                        Verdict.CE, 0, 0, 0, testCases.size());
            } else {
                outcome = testCaseEvaluator.evaluate(
                        session, testCases, problem.timeLimitMs());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not clean up the execution workspace", exception);
        }

        Instant evaluatedAt = Instant.now();
        SubmissionEvaluated result = new SubmissionEvaluated(
                UUID.randomUUID(),
                request.submissionId(),
                outcome.verdict(),
                outcome.executionTimeMs(),
                outcome.memoryUsedBytes(),
                outcome.passedTests(),
                outcome.totalTests(),
                evaluatedAt,
                1);
        judgeJobStore.complete(request, result, outcome);
    }
}
