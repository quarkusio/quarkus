package io.quarkus.aesh.deployment;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.TopCommand;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that CDI injection works in sub-commands of a {@code @GroupCommandDefinition}.
 * <p>
 * Sub-commands in {@code groupCommands} are created by Aesh via reflection, bypassing CDI.
 * The {@code AeshCdiCommandContainerBuilder} walks the command tree after construction
 * and injects CDI beans into {@code @Inject} fields of sub-command instances.
 */
public class GroupCommandCdiInjectionTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    AppGroup.class,
                    GreetSub.class,
                    GreetingService.class))
            .setApplicationName("group-cdi-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("greet", "Quarkus");

    @Test
    public void testCdiInjectionInGroupSubCommand() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .containsOnlyOnce("Hello Quarkus from CDI!");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @GroupCommandDefinition(name = "app", description = "App group", groupCommands = { GreetSub.class })
    @TopCommand
    public static class AppGroup implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Use a sub-command: greet");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "greet", description = "Greet with CDI service")
    public static class GreetSub implements Command<CommandInvocation> {

        @Argument(description = "Name to greet", required = true)
        private String name;

        @Inject
        GreetingService greetingService;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println(greetingService.greet(name));
            return CommandResult.SUCCESS;
        }
    }
}
