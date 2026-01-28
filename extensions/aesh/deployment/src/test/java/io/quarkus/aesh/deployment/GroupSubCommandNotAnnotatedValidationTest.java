package io.quarkus.aesh.deployment;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that a {@code @GroupCommandDefinition} referencing a sub-command class
 * that lacks {@code @CommandDefinition} is detected at build time.
 */
public class GroupSubCommandNotAnnotatedValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(DeploymentException.class)
            .withApplicationRoot(jar -> jar.addClasses(MyGroup.class, UnannotatedSubCommand.class));

    @Test
    public void test() {
        // Should not reach here -- deployment must fail because UnannotatedSubCommand
        // is referenced in groupCommands but lacks @CommandDefinition
    }

    @GroupCommandDefinition(name = "mygroup", description = "A group command", groupCommands = { UnannotatedSubCommand.class })
    public static class MyGroup implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    /**
     * This class implements Command but is NOT annotated with {@code @CommandDefinition},
     * which is invalid when referenced as a group sub-command.
     */
    public static class UnannotatedSubCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
