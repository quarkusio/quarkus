package io.quarkus.aesh.ssh.deployment;

import java.net.Socket;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Verifies that setting {@code quarkus.aesh.ssh.enabled=false} prevents the
 * SSH server from starting.
 */
public class AeshSshDisabledTest {

    private static final int SSH_PORT = 12225;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    HelloCommand.class,
                    GoodbyeCommand.class))
            .overrideConfigKey("quarkus.aesh.ssh.enabled", "false")
            .overrideConfigKey("quarkus.aesh.ssh.port", String.valueOf(SSH_PORT));

    @Test
    public void testSshServerNotStarted() {
        // The SSH port should not be listening when disabled
        Assertions.assertThatThrownBy(() -> {
            try (Socket socket = new Socket("localhost", SSH_PORT)) {
                // should not connect
            }
        }).isInstanceOf(java.net.ConnectException.class);
    }

    @CommandDefinition(name = "hello", description = "Say hello")
    @CliCommand
    public static class HelloCommand implements Command<CommandInvocation> {

        @Option(shortName = 'n', name = "name", defaultValue = "World")
        private String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello " + name + "!");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "goodbye", description = "Say goodbye")
    @CliCommand
    public static class GoodbyeCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Goodbye!");
            return CommandResult.SUCCESS;
        }
    }
}
