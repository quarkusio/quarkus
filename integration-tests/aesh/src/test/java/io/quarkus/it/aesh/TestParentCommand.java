package io.quarkus.it.aesh;

import static io.quarkus.it.aesh.AeshTestUtils.createConfig;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.ParentCommand;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class TestParentCommand {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("cli-app",
            CliParentCommand.class, RunSubCommand.class)
            .setCommandLineParameters("tpc-run", "build");

    @Test
    public void testParentSubCommand() {
        Assertions.assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("Running task: build");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @CommandDefinition(name = "tpc-cli", description = "CLI-like parent command", groupCommands = { RunSubCommand.class })
    public static class CliParentCommand implements Command<CommandInvocation> {

        @Option(shortName = 'v', name = "verbose", description = "Enable verbose output", hasValue = false)
        boolean verbose;

        public boolean isVerbose() {
            return verbose;
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("CLI - use a subcommand");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "tpc-run", description = "Run a task")
    public static class RunSubCommand implements Command<CommandInvocation> {

        @ParentCommand
        CliParentCommand parent;

        @Argument(description = "Task name to run")
        String taskName;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            if (parent != null && parent.isVerbose()) {
                invocation.println("[VERBOSE] Running task...");
            }
            invocation.println("Running task: " + (taskName != null ? taskName : "default"));
            return CommandResult.SUCCESS;
        }
    }
}
