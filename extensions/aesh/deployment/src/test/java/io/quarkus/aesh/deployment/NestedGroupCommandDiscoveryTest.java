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
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that {@code @CommandDefinition(groupCommands=...)} used as a sub-command of another
 * group is correctly discovered, validated, and registered in the command metadata.
 */
public class NestedGroupCommandDiscoveryTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    RootGroup.class,
                    SubGroup.class,
                    LeafCommand.class));

    @Inject
    AeshContext aeshContext;

    @Test
    public void testNestedGroupIsDiscovered() {
        // The root group should be discovered
        var rootCmd = aeshContext.getCommands().stream()
                .filter(c -> "root".equals(c.getCommandName()))
                .findFirst();
        Assertions.assertThat(rootCmd).isPresent();
        Assertions.assertThat(rootCmd.get().isGroupCommand()).isTrue();
        Assertions.assertThat(rootCmd.get().getSubCommandClassNames())
                .contains(SubGroup.class.getName());
    }

    @Test
    public void testNestedGroupSubCommandsAreDiscovered() {
        // The sub-group should also be discovered as a group command
        var subCmd = aeshContext.getCommands().stream()
                .filter(c -> "sub".equals(c.getCommandName()))
                .findFirst();
        Assertions.assertThat(subCmd).isPresent();
        Assertions.assertThat(subCmd.get().isGroupCommand()).isTrue();
        Assertions.assertThat(subCmd.get().getSubCommandClassNames())
                .contains(LeafCommand.class.getName());
    }

    @Test
    public void testLeafCommandIsDiscovered() {
        var leafCmd = aeshContext.getCommands().stream()
                .filter(c -> "leaf".equals(c.getCommandName()))
                .findFirst();
        Assertions.assertThat(leafCmd).isPresent();
        Assertions.assertThat(leafCmd.get().isGroupCommand()).isFalse();
    }

    @CommandDefinition(name = "root", description = "Root group", groupCommands = { SubGroup.class })
    public static class RootGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "sub", description = "Nested sub-group", groupCommands = { LeafCommand.class })
    public static class SubGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "leaf", description = "Leaf command")
    public static class LeafCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
