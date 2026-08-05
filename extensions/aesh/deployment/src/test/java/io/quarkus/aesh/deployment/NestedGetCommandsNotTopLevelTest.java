package io.quarkus.aesh.deployment;

import java.util.List;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommand;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests nested {@code GroupCommand.getCommands()} with {@code @Inject} children.
 * Verifies that nested sub-commands are NOT registered as top-level commands.
 *
 * Structure: root (GroupCommand) → mid (GroupCommand) → leaf
 * All children are injected via CDI and returned from getCommands().
 */
public class NestedGetCommandsNotTopLevelTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    RootGroup.class, MidGroup.class, LeafCmd.class))
            .setApplicationName("nested-getcommands-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("mid", "leaf", "Nested");

    @Test
    public void testNestedGetCommandsSubCommandsNotTopLevel() {
        // "root" is auto-detected as the top command because mid and leaf
        // are detected as subcommands via @Inject field scanning.
        // The args "mid leaf --name=Nested" resolve to root → mid → leaf.
        Assertions.assertThat(config.getStartupConsoleOutput())
                .contains("Leaf: Nested");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @CommandDefinition(name = "root", description = "Root group")
    public static class RootGroup implements GroupCommand<CommandInvocation> {

        @Inject
        MidGroup midGroup;

        @SuppressWarnings("unchecked")
        @Override
        public List<Command<CommandInvocation>> getCommands() {
            return List.of(midGroup);
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Use: mid");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mid", description = "Mid-level group")
    public static class MidGroup implements GroupCommand<CommandInvocation> {

        @Inject
        LeafCmd leafCmd;

        @SuppressWarnings("unchecked")
        @Override
        public List<Command<CommandInvocation>> getCommands() {
            return List.of(leafCmd);
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Use: leaf");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "leaf", description = "Leaf command")
    public static class LeafCmd implements Command<CommandInvocation> {

        @Argument(description = "Name", required = true)
        String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Leaf: " + name);
            return CommandResult.SUCCESS;
        }
    }
}
