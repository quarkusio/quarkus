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

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that a console mode application (multiple commands) can execute
 * a single command and exit when command-line arguments are provided.
 * <p>
 * Without arguments, the application would start the REPL.
 * With arguments, it executes the command and exits immediately.
 */
public class ConsoleModeSingleExecutionTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    GreetCmd.class,
                    CalcCmd.class,
                    GreetingService.class))
            .setApplicationName("console-single-exec-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("greet", "--name=Quarkus");

    @Test
    public void testSingleCommandExecution() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .containsOnlyOnce("Hello Quarkus from CDI!");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @CommandDefinition(name = "greet", description = "Greet someone")
    public static class GreetCmd implements Command<CommandInvocation> {

        @Option(shortName = 'n', name = "name", defaultValue = "World")
        String name;

        @Inject
        GreetingService greetingService;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println(greetingService.greet(name));
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "calc", description = "Add two numbers")
    public static class CalcCmd implements Command<CommandInvocation> {

        @Option(shortName = 'a', defaultValue = "0")
        int a;

        @Option(shortName = 'b', defaultValue = "0")
        int b;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println(a + " + " + b + " = " + (a + b));
            return CommandResult.SUCCESS;
        }
    }
}
