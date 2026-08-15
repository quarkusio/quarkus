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
 * Tests that sub-commands declared via {@code groupCommands} annotation attribute
 * are NOT registered as top-level commands. They should only be accessible
 * through their parent group.
 * <p>
 * The app has a group command "app" with sub-commands "add" and "list".
 * Running "app add test-item" should work because "add" is accessible
 * as a sub-command of "app". If "add" were also registered as a top-level
 * command, the mode auto-detection would pick up 3 top-level commands
 * (app, add, list) and enter console mode instead of runtime mode.
 */
public class SubCommandNotTopLevelTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    AppGroup.class, AddSub.class, ListSub.class))
            .setApplicationName("subcmd-not-toplevel-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("add", "test-item");

    @Test
    public void testSubCommandViaParent() {
        // With correct filtering, only "app" is a top-level command,
        // so runtime mode is auto-detected with "app" as the top command.
        // "add" is accessible as a sub-command of "app".
        Assertions.assertThat(config.getStartupConsoleOutput())
                .contains("Added: test-item");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @CommandDefinition(name = "app", description = "App group", groupCommands = { AddSub.class, ListSub.class })
    public static class AppGroup implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Use: add, list");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "add", description = "Add an item")
    public static class AddSub implements Command<CommandInvocation> {

        @Argument(description = "Item name", required = true)
        String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Added: " + name);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "list", description = "List items")
    public static class ListSub implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Items: (none)");
            return CommandResult.SUCCESS;
        }
    }
}
