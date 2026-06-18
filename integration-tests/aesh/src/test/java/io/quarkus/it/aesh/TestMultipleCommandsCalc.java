package io.quarkus.it.aesh;

import static io.quarkus.it.aesh.AeshTestUtils.createConfig;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests invoking the calc command from a multi-command application.
 */
public class TestMultipleCommandsCalc {

    @RegisterExtension
    static final QuarkusProdModeTest config = createConfig("multi-cmd-calc-app",
            AppCommand.class, GreetCommand.class, CalcCommand.class, InfoCommand.class)
            .setCommandLineParameters("tmcc-calc", "-a", "5", "-b", "3");

    @Test
    public void testCalcCommand() {
        Assertions.assertThat(config.getStartupConsoleOutput()).containsOnlyOnce("Result: 8");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @CommandDefinition(name = "tmcc-app", description = "Multi-command application", groupCommands = { GreetCommand.class,
            CalcCommand.class, InfoCommand.class })
    public static class AppCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Use a subcommand: tmcc-greet, tmcc-calc, tmcc-info");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "tmcc-greet", description = "Greet someone")
    public static class GreetCommand implements Command<CommandInvocation> {

        @Option(shortName = 'n', name = "name", description = "Name to greet", defaultValue = "Stranger")
        String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello, " + name + "!");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "tmcc-calc", description = "Calculate sum of two numbers")
    public static class CalcCommand implements Command<CommandInvocation> {

        @Option(shortName = 'a', name = "a", description = "First number", defaultValue = "0")
        int a;

        @Option(shortName = 'b', name = "b", description = "Second number", defaultValue = "0")
        int b;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Result: " + (a + b));
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "tmcc-info", description = "Show application info")
    public static class InfoCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Aesh Multi-Command App v1.0");
            return CommandResult.SUCCESS;
        }
    }
}
