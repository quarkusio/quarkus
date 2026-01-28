package io.quarkus.aesh.ssh.deployment;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests SSH password authentication for the aesh SSH extension.
 * <p>
 * Verifies that when {@code quarkus.aesh.ssh.password} is configured,
 * only connections with the correct password succeed.
 */
public class AeshSshPasswordAuthTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    HelloCommand.class,
                    GoodbyeCommand.class))
            .overrideConfigKey("quarkus.aesh.ssh.port", "12223")
            .overrideConfigKey("quarkus.aesh.ssh.password", "s3cret-test-pw");

    @Test
    public void testCorrectPasswordSucceeds() throws Exception {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect("test", "localhost", 12223)
                    .verify(10_000).getClientSession()) {
                session.addPasswordIdentity("s3cret-test-pw");
                session.auth().verify(10_000);
                // If we get here, authentication succeeded
                Assertions.assertThat(session.isAuthenticated()).isTrue();
            } finally {
                client.stop();
            }
        }
    }

    @Test
    public void testWrongPasswordFails() throws Exception {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect("test", "localhost", 12223)
                    .verify(10_000).getClientSession()) {
                session.addPasswordIdentity("wrong-password");
                Assertions.assertThatThrownBy(() -> session.auth().verify(10_000))
                        .as("Authentication with wrong password should fail")
                        .isInstanceOf(Exception.class);
            } finally {
                client.stop();
            }
        }
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
