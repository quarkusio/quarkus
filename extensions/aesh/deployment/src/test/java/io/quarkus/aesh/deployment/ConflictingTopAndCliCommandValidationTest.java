package io.quarkus.aesh.deployment;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.aesh.runtime.annotations.TopCommand;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that having both {@code @TopCommand} and {@code @CliCommand} on the same class
 * is detected at build time. These annotations represent mutually exclusive execution modes.
 */
public class ConflictingTopAndCliCommandValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(DeploymentException.class)
            .withApplicationRoot(jar -> jar.addClasses(ConflictingCommand.class));

    @Test
    public void test() {
        // Should not reach here -- deployment must fail due to conflicting annotations
    }

    @CommandDefinition(name = "conflict", description = "A command with conflicting annotations")
    @TopCommand
    @CliCommand
    public static class ConflictingCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
