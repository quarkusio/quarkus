package io.quarkus.it.aesh;

import static io.quarkus.it.aesh.AeshTestUtils.createConfig;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.option.ParentCommand;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.TopCommand;
import io.quarkus.test.QuarkusProdModeTest;

public class TestParentCommandVerbose {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("cli-verbose-app",
            CliParentCommand.class, VersionSubCommand.class)
            .setCommandLineParameters("version");

    @Test
    public void testParentSubCommandVersion() {
        Assertions.assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("Version: 1.0.0");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @GroupCommandDefinition(name = "cli", description = "CLI-like parent command", groupCommands = { VersionSubCommand.class })
    @TopCommand
    public static class CliParentCommand implements Command<CommandInvocation> {

        @Option(shortName = 'v', name = "verbose", description = "Enable verbose output", hasValue = false)
        private boolean verbose;

        public boolean isVerbose() {
            return verbose;
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("CLI - use a subcommand");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "version", description = "Show version")
    public static class VersionSubCommand implements Command<CommandInvocation> {

        @ParentCommand
        private CliParentCommand parent;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            if (parent != null && parent.isVerbose()) {
                invocation.println("[VERBOSE] Fetching version...");
            }
            invocation.println("Version: 1.0.0");
            return CommandResult.SUCCESS;
        }
    }
}
