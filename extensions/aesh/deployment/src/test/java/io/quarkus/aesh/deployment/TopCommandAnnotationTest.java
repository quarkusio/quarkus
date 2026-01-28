package io.quarkus.aesh.deployment;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that a single {@code @CommandDefinition} command is automatically
 * treated as the top-level command (auto-added {@code @TopCommand}).
 * This means the command can be invoked directly without specifying
 * its name as a subcommand.
 */
public class TopCommandAnnotationTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClass(SingleCommand.class))
            .setApplicationName("top-cmd-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("--value=42");

    @Test
    public void testSingleCommandIsAutoTopCommand() {
        // The command should work as a top command (no subcommand name needed)
        Assertions.assertThat(config.getStartupConsoleOutput())
                .containsOnlyOnce("Value: 42");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @CommandDefinition(name = "single", description = "A single command")
    public static class SingleCommand implements Command<CommandInvocation> {

        @Option(shortName = 'v', name = "value", description = "A value", defaultValue = "0")
        private String value;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Value: " + value);
            return CommandResult.SUCCESS;
        }
    }
}
