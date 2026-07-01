package io.quarkus.aesh.deployment;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests 3-level nesting of group commands using the {@code groupCommands}
 * annotation attribute. Verifies that:
 * <ul>
 * <li>Only the root command is registered as top-level</li>
 * <li>Mid-level and leaf commands are NOT top-level</li>
 * <li>The full command path {@code root mid leaf --name=...} works</li>
 * </ul>
 *
 * Structure: root → mid → leaf
 */
public class ThreeLevelNestedGroupCommandTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    RootCmd.class, MidCmd.class, LeafCmd.class))
            .setApplicationName("three-level-nested-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("mid", "leaf", "Deep");

    @Test
    public void testThreeLevelNesting() {
        // "root" is auto-detected as top command (it covers mid and leaf).
        // "mid leaf --name=Deep" is resolved as root → mid → leaf.
        Assertions.assertThat(config.getStartupConsoleOutput())
                .contains("Leaf: Deep");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @CommandDefinition(name = "root", description = "Root group", groupCommands = { MidCmd.class })
    public static class RootCmd implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Use: mid");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "mid", description = "Mid-level group", groupCommands = { LeafCmd.class })
    public static class MidCmd implements Command<CommandInvocation> {

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
