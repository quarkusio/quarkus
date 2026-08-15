package io.quarkus.aesh.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandExecutionListener;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that a CDI bean implementing {@link CommandExecutionListener} is
 * automatically discovered and receives callbacks after command execution.
 */
public class CommandExecutionListenerTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    HelloCmd.class, CalcCmd.class, MyListener.class))
            .setApplicationName("listener-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("hello", "--name=Listener");

    @Test
    public void testListenerReceivesCallback() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .contains("Hello Listener!")
                .contains("[LISTENER] command completed");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @CommandDefinition(name = "hello", description = "Greet someone")
    public static class HelloCmd implements Command<CommandInvocation> {

        @Option(shortName = 'n', name = "name", defaultValue = "World")
        String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello " + name + "!");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "calc", description = "Dummy second command")
    public static class CalcCmd implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @ApplicationScoped
    public static class MyListener implements CommandExecutionListener {

        @Override
        public void onCommandComplete(String commandLine, CommandResult result, long executionTimeMs) {
            System.out.println("[LISTENER] command completed: " + commandLine
                    + " result=" + (result.isSuccess() ? "SUCCESS" : "FAILURE")
                    + " time=" + executionTimeMs + "ms");
        }
    }
}
