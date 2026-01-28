package io.quarkus.aesh.ssh.deployment;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusUnitTest;

/**
 * End-to-end integration test for the aesh SSH terminal extension.
 * <p>
 * Connects an Apache SSHD client to the SSH server, sends a command,
 * and verifies the expected output appears in the terminal response.
 */
public class AeshSshConnectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    HelloCommand.class,
                    GoodbyeCommand.class))
            .overrideConfigKey("quarkus.aesh.ssh.port", "12222");

    @Test
    public void testSshConnection() throws Exception {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect("test", "localhost", 12222)
                    .verify(10_000).getClientSession()) {
                // No password configured — any password is accepted
                session.addPasswordIdentity("any");
                session.auth().verify(10_000);

                try (ChannelShell channel = session.createShellChannel()) {
                    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
                    channel.setOut(responseStream);
                    channel.setErr(responseStream);
                    channel.open().verify(10_000);

                    // Send the hello command
                    OutputStream pipedIn = channel.getInvertedIn();
                    pipedIn.write("hello\r".getBytes(StandardCharsets.UTF_8));
                    pipedIn.flush();

                    // Poll for expected output with a 10s deadline
                    long deadline = System.currentTimeMillis() + 10_000;
                    String output = "";
                    while (System.currentTimeMillis() < deadline) {
                        output = responseStream.toString(StandardCharsets.UTF_8);
                        if (output.contains("Hello World!")) {
                            break;
                        }
                        Thread.sleep(200);
                    }

                    Assertions.assertThat(output).contains("Hello World!");
                }
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
