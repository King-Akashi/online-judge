package com.minijudge.cli;

import com.minijudge.cli.command.HistoryCommand;
import com.minijudge.cli.command.ListProblemsCommand;
import com.minijudge.cli.command.ShowProblemCommand;
import com.minijudge.cli.command.StatusCommand;
import com.minijudge.cli.command.SubmitCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "minijudge", mixinStandardHelpOptions = true,
        version = "MiniJudge CLI 1.0.0",
        description = "Command-line client for the MiniJudge online judge.",
        subcommands = {
                ListProblemsCommand.class,
                ShowProblemCommand.class,
                SubmitCommand.class,
                StatusCommand.class,
                HistoryCommand.class
        })
public class MiniJudgeCli implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new MiniJudgeCli());
        commandLine.setExecutionExceptionHandler((exception, command, parseResult) -> {
            command.getErr().println("Error: " + exception.getMessage());
            return 1;
        });
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
}
