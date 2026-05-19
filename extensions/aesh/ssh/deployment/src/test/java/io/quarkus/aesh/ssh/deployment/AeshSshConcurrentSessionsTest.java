package io.quarkus.aesh.ssh.deployment;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
 * Verifies that multiple SSH clients can connect simultaneously and
 * each session receives correct command output independently.
 */
public class AeshSshConcurrentSessionsTest {

    private static final int SSH_PORT = 12225;
    private static final int SESSION_COUNT = 3;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(HelloCommand.class))
            .overrideConfigKey("quarkus.aesh.ssh.port", String.valueOf(SSH_PORT))
            .overrideConfigKey("quarkus.aesh.ssh.password", "test-pw");

    @Test
    public void testConcurrentSshSessions() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(SESSION_COUNT);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < SESSION_COUNT; i++) {
            futures.add(executor.submit(new SshSessionTask()));
        }

        executor.shutdown();
        Assertions.assertThat(executor.awaitTermination(30, TimeUnit.SECONDS))
                .as("All SSH sessions should complete within 30s")
                .isTrue();

        // Verify all sessions got the expected output
        for (int i = 0; i < SESSION_COUNT; i++) {
            String output = futures.get(i).get();
            Assertions.assertThat(output)
                    .as("Session %d should contain 'Hello World!'", i)
                    .contains("Hello World!");
        }

        // Verify the server is still healthy by opening one more session
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();
            try (ClientSession session = client.connect("test", "localhost", SSH_PORT)
                    .verify(10_000).getClientSession()) {
                session.addPasswordIdentity("test-pw");
                session.auth().verify(10_000);
                Assertions.assertThat(session.isAuthenticated()).isTrue();
            } finally {
                client.stop();
            }
        }
    }

    private static class SshSessionTask implements Callable<String> {
        @Override
        public String call() throws Exception {
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

                        OutputStream pipedIn = channel.getInvertedIn();
                        pipedIn.write("hello\r".getBytes(StandardCharsets.UTF_8));
                        pipedIn.flush();

                        long deadline = System.currentTimeMillis() + 10_000;
                        String output = "";
                        while (System.currentTimeMillis() < deadline) {
                            output = responseStream.toString(StandardCharsets.UTF_8);
                            if (output.contains("Hello World!")) {
                                break;
                            }
                            Thread.sleep(200);
                        }
                        return output;
                    }
                } finally {
                    client.stop();
                }
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
