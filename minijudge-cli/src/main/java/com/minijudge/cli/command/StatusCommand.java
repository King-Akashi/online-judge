package com.minijudge.cli.command;

import com.minijudge.cli.CliApiClient;
import com.minijudge.cli.dto.SubmissionResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.UUID;
import java.util.concurrent.Callable;

@Command(name = "status", mixinStandardHelpOptions = true,
        description = "Show a submission's current status and result.")
public class StatusCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Submission UUID")
    private UUID submissionId;

    @Override
    public Integer call() {
        SubmissionResponse submission = new CliApiClient().getSubmission(submissionId);
        System.out.printf("Submission: %s%n", submission.id());
        System.out.printf("Problem: %s%n", submission.problemId());
        System.out.printf("Status: %s%n", submission.status());
        if (submission.verdict() != null) {
            System.out.printf("Verdict: %s%n", submission.verdict());
            System.out.printf("Tests: %d/%d passed%n",
                    submission.passedTests(), submission.totalTests());
            System.out.printf("Execution time: %d ms%n", submission.executionTimeMs());
            if (submission.memoryUsedBytes() != null) {
                System.out.printf("Memory used: %d bytes%n", submission.memoryUsedBytes());
            }
        }
        return 0;
    }
}
