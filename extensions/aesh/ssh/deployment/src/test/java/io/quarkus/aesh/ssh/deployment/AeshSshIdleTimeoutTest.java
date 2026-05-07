package io.quarkus.aesh.ssh.deployment;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Verifies that the SSH server closes idle sessions after the configured timeout.
 */
public class AeshSshIdleTimeoutTest {

    private static final int SSH_PORT = 12227;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(HelloCommand.class))
            .overrideConfigKey("quarkus.aesh.ssh.port", String.valueOf(SSH_PORT))
            .overrideConfigKey("quarkus.aesh.ssh.password", "test-pw")
            .overrideConfigKey("quarkus.aesh.ssh.idle-timeout", "2s");

    @Test
    public void testIdleSessionIsClosed() throws Exception {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect("test", "localhost", SSH_PORT)
                    .verify(10_000).getClientSession()) {
                session.addPasswordIdentity("test-pw");
                session.auth().verify(10_000);

                try (ChannelShell channel = session.createShellChannel()) {
                    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
                    channel.setOut(responseStream);
                    channel.setErr(responseStream);
                    channel.open().verify(10_000);

                    // Send a command to verify the session works
                    OutputStream pipedIn = channel.getInvertedIn();
                    pipedIn.write("hello\r".getBytes(StandardCharsets.UTF_8));
                    pipedIn.flush();

                    long deadline = System.currentTimeMillis() + 10_000;
                    while (System.currentTimeMillis() < deadline) {
                        if (responseStream.toString(StandardCharsets.UTF_8).contains("Hello World!")) {
                            break;
                        }
                        Thread.sleep(200);
                    }
                    Assertions.assertThat(responseStream.toString(StandardCharsets.UTF_8))
                            .contains("Hello World!");

                    // Now wait for idle timeout (2s timeout + up to 1s check interval + buffer)
                    channel.waitFor(
                            EnumSet.of(ClientChannelEvent.CLOSED),
                            5_000);

                    Assertions.assertThat(channel.isClosed() || channel.isClosing())
                            .as("Session should be closed after idle timeout")
                            .isTrue();
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
}
