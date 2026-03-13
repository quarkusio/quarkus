package io.quarkus.it.aesh;

import static io.quarkus.it.aesh.AeshTestUtils.createConfig;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.TopCommand;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests invoking the calc command from a multi-command application.
 */
public class TestMultipleCommandsCalc {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("multi-cmd-calc-app",
            AppCommand.class, GreetCommand.class, CalcCommand.class, InfoCommand.class)
            .setCommandLineParameters("calc", "-a", "5", "-b", "3");

    @Test
    public void testCalcCommand() {
        Assertions.assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("Result: 8");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @GroupCommandDefinition(name = "app", description = "Multi-command application", groupCommands = { GreetCommand.class,
            CalcCommand.class, InfoCommand.class })
    @TopCommand
    public static class AppCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Use a subcommand: greet, calc, info");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "greet", description = "Greet someone")
    public static class GreetCommand implements Command<CommandInvocation> {

        @Option(shortName = 'n', name = "name", description = "Name to greet", defaultValue = "Stranger")
        private String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello, " + name + "!");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "calc", description = "Calculate sum of two numbers")
    public static class CalcCommand implements Command<CommandInvocation> {

        @Option(shortName = 'a', name = "a", description = "First number", defaultValue = "0")
        private int a;

        @Option(shortName = 'b', name = "b", description = "Second number", defaultValue = "0")
        private int b;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Result: " + (a + b));
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "info", description = "Show application info")
    public static class InfoCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Aesh Multi-Command App v1.0");
            return CommandResult.SUCCESS;
        }
    }
}
