package io.quarkus.aesh.deployment;

import java.util.List;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommand;
import org.aesh.command.invocation.CommandInvocation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that sub-commands discovered via {@code GroupCommand.getCommands()}
 * (with {@code @Inject} fields) are NOT registered as top-level commands.
 * <p>
 * The build-time processor detects {@code @Inject} fields on {@code GroupCommand}
 * implementations whose types are discovered {@code @CommandDefinition} classes,
 * and excludes them from top-level registration.
 */
public class GetCommandsSubCommandNotTopLevelTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    ToolsGroup.class, StatusSub.class, VersionSub.class, InfoService.class))
            .setApplicationName("getcommands-subcmd-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("status");

    @Test
    public void testGetCommandsSubCommandViaParent() {
        // With correct filtering, only "tools" is a top-level command,
        // so runtime mode is auto-detected. "status" is a sub-command
        // of "tools", accessible via "tools status".
        Assertions.assertThat(config.getStartupConsoleOutput())
                .contains("Info: status (from CDI)");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @CommandDefinition(name = "tools", description = "Developer tools")
    public static class ToolsGroup implements GroupCommand<CommandInvocation> {

        @Inject
        StatusSub statusSub;

        @Inject
        VersionSub versionSub;

        @SuppressWarnings("unchecked")
        @Override
        public List<Command<CommandInvocation>> getCommands() {
            return List.of(statusSub, versionSub);
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Use: status, version");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "status", description = "Show status")
    public static class StatusSub implements Command<CommandInvocation> {

        @Inject
        InfoService infoService;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println(infoService.getInfo("status"));
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "version", description = "Show version")
    public static class VersionSub implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Version: 1.0.0");
            return CommandResult.SUCCESS;
        }
    }
}
