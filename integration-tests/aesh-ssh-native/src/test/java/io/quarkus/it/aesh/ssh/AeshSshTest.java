package io.quarkus.it.aesh.ssh;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AeshSshTest {

    @Test
    public void testSshConnectionAndCommand() throws Exception {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect("test", "localhost", 12240)
                    .verify(10_000).getClientSession()) {
                session.addPasswordIdentity("any");
                session.auth().verify(10_000);

                try (ChannelShell channel = session.createShellChannel()) {
                    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
                    channel.setOut(responseStream);
                    channel.setErr(responseStream);
                    channel.open().verify(10_000);

                    OutputStream pipedIn = channel.getInvertedIn();
                    pipedIn.write("hello --name=Native\r".getBytes(StandardCharsets.UTF_8));
                    pipedIn.flush();

                    long deadline = System.currentTimeMillis() + 10_000;
                    String output = "";
                    while (System.currentTimeMillis() < deadline) {
                        output = responseStream.toString(StandardCharsets.UTF_8);
                        if (output.contains("Hello Native!")) {
                            break;
                        }
                        Thread.sleep(200);
                    }

                    Assertions.assertThat(output).contains("Hello Native!");
                }
            } finally {
                client.stop();
            }
        }
    }

    @Test
    public void testCdiInjectionOverSsh() throws Exception {
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect("test", "localhost", 12240)
                    .verify(10_000).getClientSession()) {
                session.addPasswordIdentity("any");
                session.auth().verify(10_000);

                try (ChannelShell channel = session.createShellChannel()) {
                    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
                    channel.setOut(responseStream);
                    channel.setErr(responseStream);
                    channel.open().verify(10_000);

                    OutputStream pipedIn = channel.getInvertedIn();
                    pipedIn.write("cdi-greet --name=CDI\r".getBytes(StandardCharsets.UTF_8));
                    pipedIn.flush();

                    long deadline = System.currentTimeMillis() + 10_000;
                    String output = "";
                    while (System.currentTimeMillis() < deadline) {
                        output = responseStream.toString(StandardCharsets.UTF_8);
                        if (output.contains("Hello CDI from service!")) {
                            break;
                        }
                        Thread.sleep(200);
                    }

                    Assertions.assertThat(output).contains("Hello CDI from service!");
                }
            } finally {
                client.stop();
            }
        }
    }
}
