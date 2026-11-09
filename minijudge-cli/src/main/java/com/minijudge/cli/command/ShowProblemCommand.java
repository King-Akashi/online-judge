package com.minijudge.cli.command;

import com.minijudge.cli.CliApiClient;
import com.minijudge.cli.dto.ProblemDetails;
import com.minijudge.cli.dto.SampleTestCase;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.UUID;
import java.util.concurrent.Callable;

@Command(name = "show", mixinStandardHelpOptions = true,
        description = "Show a problem statement and sample tests.")
public class ShowProblemCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Problem UUID")
    private UUID problemId;

    @Override
    public Integer call() {
        ProblemDetails problem = new CliApiClient().getProblem(problemId);
        System.out.printf("%s (%s, %s)%n", problem.title(), problem.difficulty(), problem.language());
        System.out.printf("Time limit: %d ms | Memory limit: %d MB%n%n",
                problem.timeLimitMs(), problem.memoryLimitMb());
        System.out.println(problem.statement());
        System.out.println();
        System.out.println("Constraints:");
        System.out.println(problem.constraints());
        System.out.println();
        System.out.println("Starter code:");
        System.out.println(problem.starterCode());
        for (int index = 0; index < problem.sampleTestCases().size(); index++) {
            SampleTestCase testCase = problem.sampleTestCases().get(index);
            System.out.printf("%nSample %d input:%n%s", index + 1, testCase.input());
            System.out.printf("Sample %d output:%n%s", index + 1, testCase.expectedOutput());
        }
        return 0;
    }
}
