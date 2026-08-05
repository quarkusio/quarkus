package io.quarkus.aesh.ssh.deployment;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Verifies that the SSH health check reports the correct active connection count
 * when sessions are open.
 */
public class AeshSshHealthActiveConnectionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(HelloCommand.class))
            .overrideConfigKey("quarkus.aesh.ssh.port", "12235")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Test
    public void testHealthReportsActiveConnections() throws Exception {
        // First verify 0 active connections
        RestAssured.when().get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("checks.find { it.name == 'Aesh SSH server health check' }.data.activeConnections",
                        CoreMatchers.equalTo(0));

        // Open an SSH session
        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client.connect("test", "localhost", 12235)
                    .verify(10_000).getClientSession()) {
                session.addPasswordIdentity("any");
                session.auth().verify(10_000);

                try (ChannelShell channel = session.createShellChannel()) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    channel.setOut(out);
                    channel.setErr(out);
                    channel.open().verify(10_000);

                    // Wait for the connection to be fully established
                    long deadline = System.currentTimeMillis() + 5_000;
                    while (System.currentTimeMillis() < deadline) {
                        String output = out.toString(StandardCharsets.UTF_8);
                        if (output.contains("[quarkus]")) {
                            break;
                        }
                        Thread.sleep(200);
                    }

                    // Now check health — should report 1 active connection
                    String healthBody = RestAssured.when().get("/q/health/ready")
                            .then()
                            .statusCode(200)
                            .extract().body().asString();

                    Assertions.assertThat(healthBody).contains("\"activeConnections\"");
                    // Parse to verify the value
                    RestAssured.when().get("/q/health/ready")
                            .then()
                            .body("checks.find { it.name == 'Aesh SSH server health check' }.data.activeConnections",
                                    Matchers.greaterThanOrEqualTo(1));
                }
            } finally {
                client.stop();
            }
        }

        // After disconnecting, wait briefly and verify count drops back to 0
        Thread.sleep(500);
        RestAssured.when().get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("checks.find { it.name == 'Aesh SSH server health check' }.data.activeConnections",
                        CoreMatchers.equalTo(0));
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
