package io.quarkus.aesh.websocket.deployment;

import java.net.URI;
import java.util.concurrent.CopyOnWriteArrayList;
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

import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

/**
 * End-to-end integration test for the aesh WebSocket terminal extension.
 * <p>
 * Connects a Vert.x WebSocket client to the {@code /aesh/terminal} endpoint,
 * sends an init message followed by a command, and verifies the expected output
 * appears in the terminal response.
 */
public class AeshWebSocketConnectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    HelloCommand.class,
                    GoodbyeCommand.class));

    @TestHTTPResource("/aesh/terminal")
    URI wsUri;

    @Inject
    Vertx vertx;

    @Test
    public void testWebSocketConnection() throws Exception {
        CopyOnWriteArrayList<String> messages = new CopyOnWriteArrayList<>();
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

            ws.textMessageHandler(msg -> {
                messages.add(msg);
                if (msg.contains("Hello World!")) {
                    latch.countDown();
                }
            });

            // Send init message to set up the terminal
            ws.writeTextMessage("{\"action\":\"init\",\"cols\":80,\"rows\":24}");

            // After a short delay, send the hello command
            vertx.setTimer(500, id -> {
                ws.writeTextMessage("{\"action\":\"read\",\"data\":\"hello\\r\"}");
            });
        });

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        String allOutput = String.join("", messages);

        Assertions.assertThat(completed)
                .as("Expected to receive 'Hello World!' in WebSocket output within 10s. Received: %s", allOutput)
                .isTrue();
        Assertions.assertThat(allOutput).contains("Hello World!");
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
