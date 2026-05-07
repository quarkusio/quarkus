package io.quarkus.aesh.deployment;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that a command throwing an exception results in exit code 1.
 */
public class CommandExceptionExitCodeTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClass(ExplodingCommand.class))
            .setApplicationName("explode-exit-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true);

    @Test
    public void testExceptionExitCode() {
        Assertions.assertThat(config.getExitCode()).isEqualTo(1);
    }

    @CommandDefinition(name = "explode", description = "A command that always throws")
    public static class ExplodingCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            throw new RuntimeException("Boom!");
        }
    }
}
