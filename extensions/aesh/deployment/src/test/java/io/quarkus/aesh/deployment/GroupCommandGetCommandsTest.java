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
 * Tests that the {@link GroupCommand#getCommands()} pattern works with the
 * aesh extension, including CDI injection in child commands.
 * <p>
 * This pattern is used by quarkiverse extensions (e.g. quarkus-operator-sdk)
 * and by the Quarkus dev console (DevServicesCommand) to register child
 * commands programmatically instead of via {@code groupCommands} annotation
 * attribute.
 */
public class GroupCommandGetCommandsTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    ToolsGroup.class,
                    InfoSubCommand.class,
                    InfoService.class))
            .setApplicationName("group-getcommands-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.aesh.mode", "runtime")
            .overrideConfigKey("quarkus.aesh.top-command",
                    "io.quarkus.aesh.deployment.GroupCommandGetCommandsTest$ToolsGroup")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("info", "Quarkus");

    @Test
    public void testGetCommandsWithCdiInjection() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .containsOnlyOnce("Info: Quarkus (from CDI)");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @CommandDefinition(name = "tools", description = "Tools group command")
    public static class ToolsGroup implements GroupCommand<CommandInvocation> {

        @Inject
        InfoSubCommand infoSub;

        @SuppressWarnings("unchecked")
        @Override
        public List<Command<CommandInvocation>> getCommands() {
            return List.of(infoSub);
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Use a sub-command: info");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "info", description = "Show info")
    public static class InfoSubCommand implements Command<CommandInvocation> {

        @Argument(description = "Topic", required = true)
        String topic;

        @Inject
        InfoService infoService;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println(infoService.getInfo(topic));
            return CommandResult.SUCCESS;
        }
    }
}
