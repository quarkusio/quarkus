package io.quarkus.aesh.deployment;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.AeshContext;
import io.quarkus.aesh.runtime.AeshMode;
import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that {@code quarkus.aesh.start-console=false} disables the local console
 * even when no remote transports are present. This is useful for embedding CLI
 * commands in a server application without interactive console access.
 */
public class StartConsoleDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(HelloCommand.class))
            .overrideConfigKey("quarkus.aesh.start-console", "false");

    @Inject
    AeshContext aeshContext;

    @Test
    public void testApplicationStartsWithoutConsole() {
        // With start-console=false, no CliRunner/QuarkusApplication is registered.
        // The test completing proves no local console blocked the app.
        Assertions.assertThat(aeshContext).isNotNull();
        Assertions.assertThat(aeshContext.getMode()).isEqualTo(AeshMode.console);
    }

    @Test
    public void testCommandsStillDiscovered() {
        Assertions.assertThat(aeshContext.getCommands()).isNotEmpty();
    }

    @CommandDefinition(name = "hello", description = "Hello command")
    @CliCommand
    public static class HelloCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello");
            return CommandResult.SUCCESS;
        }
    }
}
