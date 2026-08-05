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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

/**
 * Verifies the WebSocket endpoint handles malformed and unexpected messages
 * gracefully without crashing or leaking connections.
 */
public class AeshWebSocketMalformedMessageTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(HelloCommand.class));

    @TestHTTPResource("/aesh/terminal")
    URI wsUri;

    @Inject
    Vertx vertx;

    @Test
    public void testInvalidJsonDoesNotCrashEndpoint() throws Exception {
        // Send invalid JSON as the first message, then valid init + command.
        // The connection should survive the bad message and still work.
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
                if (msg.contains("Hello!")) {
                    latch.countDown();
                }
            });

            // Send invalid JSON -- should be handled gracefully
            ws.writeTextMessage("this is not json at all");

            // After a short delay, send valid init + command
            vertx.setTimer(500, id -> {
                ws.writeTextMessage("{\"action\":\"init\",\"cols\":80,\"rows\":24}");
                vertx.setTimer(500, id2 -> {
                    ws.writeTextMessage("{\"action\":\"read\",\"data\":\"hello\\r\"}");
                });
            });
        });

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        String allOutput = String.join("", messages);

        Assertions.assertThat(completed)
                .as("Connection should survive invalid JSON and process subsequent valid messages. Output: %s",
                        allOutput)
                .isTrue();
        Assertions.assertThat(allOutput).contains("Hello!");
    }

    @Test
    public void testUnknownActionDoesNotCrashEndpoint() throws Exception {
        // Send a valid JSON message with an unknown action, then a valid command.
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
                if (msg.contains("Hello!")) {
                    latch.countDown();
                }
            });

            // Valid JSON init first (so connection is established)
            ws.writeTextMessage("{\"action\":\"init\",\"cols\":80,\"rows\":24}");

            vertx.setTimer(500, id -> {
                // Unknown action -- should be silently ignored
                ws.writeTextMessage("{\"action\":\"unknown_action\",\"data\":\"something\"}");

                vertx.setTimer(300, id2 -> {
                    // Valid command should still work
                    ws.writeTextMessage("{\"action\":\"read\",\"data\":\"hello\\r\"}");
                });
            });
        });

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        String allOutput = String.join("", messages);

        Assertions.assertThat(completed)
                .as("Connection should survive unknown actions and process subsequent valid messages. Output: %s",
                        allOutput)
                .isTrue();
        Assertions.assertThat(allOutput).contains("Hello!");
    }

    @Test
    public void testEmptyMessageDoesNotCrashEndpoint() throws Exception {
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
                if (msg.contains("Hello!")) {
                    latch.countDown();
                }
            });

            // Empty string -- should be handled gracefully
            ws.writeTextMessage("");

            vertx.setTimer(500, id -> {
                ws.writeTextMessage("{\"action\":\"init\",\"cols\":80,\"rows\":24}");
                vertx.setTimer(500, id2 -> {
                    ws.writeTextMessage("{\"action\":\"read\",\"data\":\"hello\\r\"}");
                });
            });
        });

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        String allOutput = String.join("", messages);

        Assertions.assertThat(completed)
                .as("Connection should survive empty messages. Output: %s", allOutput)
                .isTrue();
        Assertions.assertThat(allOutput).contains("Hello!");
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
