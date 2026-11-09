package com.minijudge.cli.command;

import com.minijudge.cli.CliApiClient;
import com.minijudge.cli.dto.SubmissionResponse;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "history", mixinStandardHelpOptions = true,
        description = "List the most recent submissions.")
public class HistoryCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        for (SubmissionResponse submission : new CliApiClient().submissionHistory()) {
            System.out.printf("%s  %-10s %-4s  %s  %s%n",
                    submission.id(),
                    submission.status(),
                    submission.verdict() == null ? "-" : submission.verdict(),
                    submission.problemId(),
                    submission.createdAt());
        }
        return 0;
    }
}
