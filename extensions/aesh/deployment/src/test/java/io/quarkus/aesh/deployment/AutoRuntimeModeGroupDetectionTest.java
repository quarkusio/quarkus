package io.quarkus.aesh.deployment;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.AeshContext;
import io.quarkus.aesh.runtime.AeshMode;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Verifies that {@code quarkus.aesh.mode=auto} (the default) with a single
 * group command that covers all subcommands resolves to runtime mode.
 * The group command should be detected as the top command.
 */
public class AutoRuntimeModeGroupDetectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    AppCommand.class,
                    RunSubCommand.class,
                    BuildSubCommand.class))
            .overrideConfigKey("quarkus.aesh.mode", "auto");

    @Inject
    AeshContext aeshContext;

    @Test
    public void testGroupCoveringAllAutoDetectsRuntimeMode() {
        Assertions.assertThat(aeshContext.getMode())
                .as("Group covering all subcommands should auto-detect as runtime mode")
                .isEqualTo(AeshMode.runtime);
    }

    @Test
    public void testTopCommandIsGroupCommand() {
        Assertions.assertThat(aeshContext.getTopCommandClassName())
                .as("Top command should be the group command")
                .isEqualTo(AppCommand.class.getName());
    }

    @CommandDefinition(name = "app", description = "App CLI", groupCommands = { RunSubCommand.class, BuildSubCommand.class })
    public static class AppCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("app root");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "run", description = "Run the app")
    public static class RunSubCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("running");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "build", description = "Build the app")
    public static class BuildSubCommand implements Command<CommandInvocation> {
        @Option(name = "target", defaultValue = "jar")
        String target;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("building " + target);
            return CommandResult.SUCCESS;
        }
    }
}
