package io.quarkus.aesh.ssh.deployment;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Verifies that sending commands resets the idle timeout timer,
 * preventing active sessions from being disconnected.
 */
public class AeshSshIdleActivityResetTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(HelloCommand.class))
            .overrideConfigKey("quarkus.aesh.ssh.port", "12237")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideRuntimeConfigKey("quarkus.aesh.ssh.idle-timeout", "2s");

    @Test
    public void testActivityPreventsIdleTimeout() throws Exception {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect("test", "localhost", 12237)
                    .verify(10_000).getClientSession()) {
                session.addPasswordIdentity("any");
                session.auth().verify(10_000);

                try (ChannelShell channel = session.createShellChannel()) {
                    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
                    channel.setOut(responseStream);
                    channel.setErr(responseStream);
                    channel.open().verify(10_000);

                    OutputStream pipedIn = channel.getInvertedIn();

                    // Wait for prompt
                    long deadline = System.currentTimeMillis() + 5_000;
                    while (System.currentTimeMillis() < deadline) {
                        if (responseStream.toString(StandardCharsets.UTF_8).contains("[quarkus]")) {
                            break;
                        }
                        Thread.sleep(200);
                    }

                    // Send commands every 1.5s for 5 seconds total.
                    // The idle timeout is 2s, so without activity reset,
                    // the session would be closed after the 2nd interval.
                    for (int i = 0; i < 4; i++) {
                        Thread.sleep(1500);
                        pipedIn.write("hello\r".getBytes(StandardCharsets.UTF_8));
                        pipedIn.flush();
                    }

                    // Wait a bit for the last response
                    Thread.sleep(500);
                    String output = responseStream.toString(StandardCharsets.UTF_8);

                    // The session should still be alive and have received all responses.
                    // If idle timeout wasn't reset, the connection would have been dropped
                    // after ~2s and we'd see "Connection closed" or fewer "Hello!" responses.
                    long helloCount = output.lines()
                            .filter(line -> line.contains("Hello!"))
                            .count();
                    Assertions.assertThat(helloCount)
                            .as("All 4 hello commands should have produced output (activity resets idle timer)")
                            .isGreaterThanOrEqualTo(4);

                    Assertions.assertThat(channel.isOpen())
                            .as("Channel should still be open after continuous activity")
                            .isTrue();
                }
            } finally {
                client.stop();
            }
        }
    }

    @CommandDefinition(name = "hello", description = "Say hello")
    public static class HelloCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello!");
            return CommandResult.SUCCESS;
        }
    }
}
