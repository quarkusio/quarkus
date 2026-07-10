package io.quarkus.aesh.websocket.deployment;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

/**
 * End-to-end integration test for the aesh WebSocket terminal extension.
 */
public class AeshWebSocketConnectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    AeshWebSocketTestHelper.class,
                    HelloCommand.class,
                    GoodbyeCommand.class));

    @TestHTTPResource("/aesh/terminal")
    URI wsUri;

    @Inject
    Vertx vertx;

    @Test
    public void testWebSocketConnection() throws Exception {
        StringBuilder output = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setHost(wsUri.getHost())
                .setPort(wsUri.getPort())
                .setURI(wsUri.getPath());

        client.connect(options).onComplete(ar -> {
            if (ar.failed()) {
                latch.countDown();
                return;
            }
            var ws = ar.result();
            AeshWebSocketTestHelper.sendCommandOnPrompt(ws, "hello", "Hello World!", output, latch);
            ws.writeTextMessage("{\"action\":\"init\",\"cols\":80,\"rows\":24}");
        });

        boolean completed = latch.await(30, TimeUnit.SECONDS);

        Assertions.assertThat(completed)
                .as("Expected to receive 'Hello World!' in WebSocket output within 30s. Received: %s", output)
                .isTrue();
        Assertions.assertThat(output.toString()).contains("Hello World!");
    }

    @CommandDefinition(name = "hello", description = "Say hello")
    public static class HelloCommand implements Command<CommandInvocation> {

        @Option(shortName = 'n', name = "name", defaultValue = "World")
        String name;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello " + name + "!");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "goodbye", description = "Say goodbye")
    public static class GoodbyeCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Goodbye!");
            return CommandResult.SUCCESS;
        }
    }
}
