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
 * Tests that a command returning {@link CommandResult#FAILURE} results in exit code 1.
 */
public class CommandFailureExitCodeTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClass(FailableCommand.class))
            .setApplicationName("fail-exit-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("--fail");

    @Test
    public void testFailureExitCode() {
        Assertions.assertThat(config.getExitCode()).isEqualTo(1);
    }

    @CommandDefinition(name = "failable", description = "A command that can fail")
    public static class FailableCommand implements Command<CommandInvocation> {

        @Option(name = "fail", hasValue = false, description = "Whether to fail")
        private boolean fail;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            if (fail) {
                return CommandResult.FAILURE;
            }
            invocation.println("OK");
            return CommandResult.SUCCESS;
        }
    }
}
