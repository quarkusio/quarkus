package io.quarkus.aesh.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandNotFoundHandler;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.shell.Shell;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that a CDI bean implementing {@link CommandNotFoundHandler} is
 * automatically discovered and called when an unknown command is typed.
 */
public class CommandNotFoundHandlerTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    HelloCmd.class, CalcCmd.class, MyNotFoundHandler.class))
            .setApplicationName("notfound-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("doesnotexist");

    @Test
    public void testNotFoundHandlerCalled() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .contains("Unknown command: doesnotexist. Try: hello, calc");
        // Command not found should result in exit code 1
        Assertions.assertThat(config.getExitCode()).isEqualTo(1);
    }

    @CommandDefinition(name = "hello", description = "Greet someone")
    public static class HelloCmd implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello!");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "calc", description = "Calculator")
    public static class CalcCmd implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @ApplicationScoped
    public static class MyNotFoundHandler implements CommandNotFoundHandler {

        @Override
        public void handleCommandNotFound(String line, Shell shell) {
            shell.writeln("Unknown command: " + line + ". Try: hello, calc");
        }
    }
}
