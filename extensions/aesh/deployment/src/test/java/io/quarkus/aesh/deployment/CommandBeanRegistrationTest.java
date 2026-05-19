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
 * Tests that a command annotated with {@code @CommandDefinition} is automatically
 * registered as a CDI bean without requiring an explicit scope annotation.
 */
public class CommandBeanRegistrationTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClass(AutoRegisteredCommand.class))
            .setApplicationName("auto-register-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("--message=AutoBean");

    @Test
    public void testCommandIsAutoRegistered() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .containsOnlyOnce("Message: AutoBean");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @CommandDefinition(name = "auto", description = "Auto-registered command")
    public static class AutoRegisteredCommand implements Command<CommandInvocation> {

        @Option(shortName = 'm', name = "message", description = "Message to print", defaultValue = "default")
        private String message;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Message: " + message);
            return CommandResult.SUCCESS;
        }
    }
}
