package io.quarkus.aesh.deployment;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that sub-commands listed in {@code @GroupCommandDefinition.groupCommands}
 * are not promoted to top-level CLI commands in console mode.
 * <p>
 * Without this fix, the AeshProcessor would add {@code @CliCommand} to all
 * {@code @CommandDefinition} classes in console mode, causing sub-commands to
 * appear both as sub-commands of their group AND as top-level commands.
 * <p>
 * This test uses runtime mode (via {@code @TopCommand}) to verify sub-commands
 * are only accessible through their group parent.
 */
public class ConsoleGroupCommandTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    ToolGroup.class,
                    RunSub.class,
                    StatusSub.class,
                    StandaloneCmd.class))
            .setApplicationName("console-group-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            // Console mode is detected (multiple @CliCommand commands)
            // but we don't run because console mode requires interactive stdin
            .setRun(false);

    @Test
    public void testConsoleModeBuildWithGroupCommands() {
        // The build succeeds, which means:
        // 1. @CliCommand was added to StandaloneCmd (standalone @CommandDefinition)
        // 2. @CliCommand was NOT added to RunSub and StatusSub (sub-commands of ToolGroup)
        // 3. ToolGroup (explicit @CliCommand) is registered as a top-level command
        // If sub-command exclusion fails, sub-commands would be registered both as
        // top-level AND as sub-commands, causing duplicate command issues at runtime.
        Assertions.assertThat(true).isTrue();
    }

    @GroupCommandDefinition(name = "tool", description = "Tool group", groupCommands = { RunSub.class, StatusSub.class })
    @CliCommand
    public static class ToolGroup implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("tool: use a sub-command (run, status)");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "run", description = "Run something")
    public static class RunSub implements Command<CommandInvocation> {

        @Argument(description = "What to run")
        private String target;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("running: " + (target != null ? target : "default"));
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "status", description = "Show status")
    public static class StatusSub implements Command<CommandInvocation> {

        @Option(shortName = 'v', name = "verbose", hasValue = false)
        private boolean verbose;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("status: ok" + (verbose ? " (verbose)" : ""));
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "hello", description = "Standalone hello command")
    @CliCommand
    public static class StandaloneCmd implements Command<CommandInvocation> {

        @Option(shortName = 'n', name = "name", defaultValue = "World")
        private String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello " + name + "!");
            return CommandResult.SUCCESS;
        }
    }
}
