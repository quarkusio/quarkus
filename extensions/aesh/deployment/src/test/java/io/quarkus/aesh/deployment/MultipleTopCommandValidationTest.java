package io.quarkus.aesh.deployment;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.TopCommand;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that having multiple {@code @TopCommand} annotations is detected at build time.
 * Only one command can serve as the top-level entry point in runtime mode.
 */
public class MultipleTopCommandValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(DeploymentException.class)
            .withApplicationRoot(jar -> jar.addClasses(TopCommandA.class, TopCommandB.class));

    @Test
    public void test() {
        // Should not reach here -- deployment must fail due to multiple @TopCommand annotations
    }

    @CommandDefinition(name = "alpha", description = "First top command")
    @TopCommand
    public static class TopCommandA implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "beta", description = "Second top command")
    @TopCommand
    public static class TopCommandB implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
