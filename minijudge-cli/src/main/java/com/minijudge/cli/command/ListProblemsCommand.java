package com.minijudge.cli.command;

import com.minijudge.cli.CliApiClient;
import com.minijudge.cli.dto.ProblemSummary;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "list", mixinStandardHelpOptions = true, description = "List available problems.")
public class ListProblemsCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        CliApiClient client = new CliApiClient();
        for (ProblemSummary problem : client.listProblems()) {
            System.out.printf("%-36s %-8s %-8s %s%n",
                    problem.id(), problem.difficulty(), problem.language(), problem.title());
        }
        return 0;
    }
}
