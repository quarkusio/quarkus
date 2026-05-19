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
 * Verifies that the SSH server enforces the max-connections limit.
 */
public class AeshSshMaxConnectionsTest {

    private static final int SSH_PORT = 12226;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(HelloCommand.class))
            .overrideConfigKey("quarkus.aesh.ssh.port", String.valueOf(SSH_PORT))
            .overrideConfigKey("quarkus.aesh.ssh.password", "test-pw")
            .overrideConfigKey("quarkus.aesh.ssh.max-connections", "2");

    @Test
    public void testMaxConnectionsEnforced() throws Exception {
        SshClient client1 = SshClient.setUpDefaultClient();
        SshClient client2 = SshClient.setUpDefaultClient();
        SshClient client3 = SshClient.setUpDefaultClient();
        try {
            client1.start();
            client2.start();
            client3.start();

            // Open first session and verify it works
            ClientSession session1 = client1.connect("test", "localhost", SSH_PORT)
                    .verify(10_000).getClientSession();
            session1.addPasswordIdentity("test-pw");
            session1.auth().verify(10_000);
            ChannelShell channel1 = session1.createShellChannel();
            ByteArrayOutputStream out1 = new ByteArrayOutputStream();
            channel1.setOut(out1);
            channel1.setErr(out1);
            channel1.open().verify(10_000);

            OutputStream in1 = channel1.getInvertedIn();
            in1.write("hello\r".getBytes(StandardCharsets.UTF_8));
            in1.flush();
            waitForOutput(out1, "Hello World!", 10_000);

            // Open second session and verify it works
            ClientSession session2 = client2.connect("test", "localhost", SSH_PORT)
                    .verify(10_000).getClientSession();
            session2.addPasswordIdentity("test-pw");
            session2.auth().verify(10_000);
            ChannelShell channel2 = session2.createShellChannel();
            ByteArrayOutputStream out2 = new ByteArrayOutputStream();
            channel2.setOut(out2);
            channel2.setErr(out2);
            channel2.open().verify(10_000);

            OutputStream in2 = channel2.getInvertedIn();
            in2.write("hello\r".getBytes(StandardCharsets.UTF_8));
            in2.flush();
            waitForOutput(out2, "Hello World!", 10_000);

            // Third session should be rejected
            ClientSession session3 = client3.connect("test", "localhost", SSH_PORT)
                    .verify(10_000).getClientSession();
            session3.addPasswordIdentity("test-pw");
            session3.auth().verify(10_000);
            ChannelShell channel3 = session3.createShellChannel();
            ByteArrayOutputStream out3 = new ByteArrayOutputStream();
            channel3.setOut(out3);
            channel3.setErr(out3);
            channel3.open().verify(10_000);

            // Verify the rejection message is received
            waitForOutput(out3, "Connection rejected", 10_000);
            Assertions.assertThat(out3.toString(StandardCharsets.UTF_8))
                    .contains("Connection rejected");

            // Cleanup third session
            channel3.close();
            session3.close();

            // Close first session
            channel1.close();
            session1.close();

            // Give the server a moment to update the counter
            Thread.sleep(1000);

            // Now a new session should succeed
            SshClient client4 = SshClient.setUpDefaultClient();
            try {
                client4.start();
                ClientSession session4 = client4.connect("test", "localhost", SSH_PORT)
                        .verify(10_000).getClientSession();
                session4.addPasswordIdentity("test-pw");
                session4.auth().verify(10_000);
                ChannelShell channel4 = session4.createShellChannel();
                ByteArrayOutputStream out4 = new ByteArrayOutputStream();
                channel4.setOut(out4);
                channel4.setErr(out4);
                channel4.open().verify(10_000);

                OutputStream in4 = channel4.getInvertedIn();
                in4.write("hello\r".getBytes(StandardCharsets.UTF_8));
                in4.flush();
                waitForOutput(out4, "Hello World!", 10_000);

                channel4.close();
                session4.close();
            } finally {
                client4.stop();
            }

            // Cleanup
            channel2.close();
            session2.close();
        } finally {
            client1.stop();
            client2.stop();
            client3.stop();
        }
    }

    private static void waitForOutput(ByteArrayOutputStream stream, String expected, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (stream.toString(StandardCharsets.UTF_8).contains(expected)) {
                return;
            }
            Thread.sleep(200);
        }
        Assertions.assertThat(stream.toString(StandardCharsets.UTF_8)).contains(expected);
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
