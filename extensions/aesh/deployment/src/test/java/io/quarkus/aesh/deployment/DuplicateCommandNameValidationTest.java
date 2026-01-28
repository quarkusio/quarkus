package io.quarkus.aesh.deployment;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that duplicate top-level command names are detected at build time.
 * Two commands with the same {@code name} in {@code @CommandDefinition}
 * should cause a deployment failure.
 */
public class DuplicateCommandNameValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(DeploymentException.class)
            .withApplicationRoot(jar -> jar.addClasses(GreetCommandA.class, GreetCommandB.class));

    @Test
    public void test() {
        // Should not reach here -- deployment must fail due to duplicate command name "greet"
    }

    @CommandDefinition(name = "greet", description = "First greet command")
    public static class GreetCommandA implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "greet", description = "Second greet command")
    public static class GreetCommandB implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
