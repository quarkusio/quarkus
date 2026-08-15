package io.quarkus.aesh.websocket.deployment;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

/**
 * Verifies that the WebSocket endpoint enforces the max-connections limit.
 * <p>
 * The test opens connections sequentially (not concurrently) to avoid
 * race conditions. Each connection is fully established before the next
 * one is attempted. The third connection should be rejected because
 * the max-connections limit (2) has been reached.
 */
public class AeshWebSocketMaxConnectionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(HelloCommand.class))
            .overrideConfigKey("quarkus.aesh.websocket.max-connections", "2");

    @TestHTTPResource("/aesh/terminal")
    URI wsUri;

    @Inject
    Vertx vertx;

    @Test
    public void testMaxConnectionsEnforced() throws Exception {
        List<WebSocket> openSockets = new ArrayList<>();
        WebSocketClient client = vertx.createWebSocketClient();

        try {
            // Open two connections sequentially -- each must succeed
            for (int i = 0; i < 2; i++) {
                CountDownLatch connected = new CountDownLatch(1);
                AtomicBoolean success = new AtomicBoolean(false);

                WebSocketConnectOptions options = new WebSocketConnectOptions()
                        .setHost(wsUri.getHost())
                        .setPort(wsUri.getPort())
                        .setURI(wsUri.getPath());

                client.connect(options).onComplete(ar -> {
                    if (ar.succeeded()) {
                        var ws = ar.result();
                        openSockets.add(ws);
                        // Send init to register the session on the server
                        ws.writeTextMessage("{\"action\":\"init\",\"cols\":80,\"rows\":24}");
                        success.set(true);
                    }
                    connected.countDown();
                });

                boolean ok = connected.await(30, TimeUnit.SECONDS);
                Assertions.assertThat(ok).as("Connection %d should complete within 30s", i + 1).isTrue();
                Assertions.assertThat(success.get()).as("Connection %d should succeed", i + 1).isTrue();

                // Small delay to ensure the server processes the init message
                // and registers the session before opening the next connection
                Thread.sleep(500);
            }

            // Third connection should be rejected or closed by the server
            CountDownLatch thirdDone = new CountDownLatch(1);
            AtomicBoolean thirdRejected = new AtomicBoolean(false);

            WebSocketConnectOptions options3 = new WebSocketConnectOptions()
                    .setHost(wsUri.getHost())
                    .setPort(wsUri.getPort())
                    .setURI(wsUri.getPath());

            client.connect(options3).onComplete(ar -> {
                if (ar.failed()) {
                    // Connection rejected at handshake level
                    thirdRejected.set(true);
                    thirdDone.countDown();
                    return;
                }
                var ws = ar.result();
                ws.closeHandler(v -> {
                    // Server closed the connection after init
                    thirdRejected.set(true);
                    thirdDone.countDown();
                });
                // Send init to trigger the max-connections check on the server
                ws.writeTextMessage("{\"action\":\"init\",\"cols\":80,\"rows\":24}");
            });

            boolean done = thirdDone.await(30, TimeUnit.SECONDS);
            Assertions.assertThat(done).as("Third connection attempt should complete within 30s").isTrue();
            Assertions.assertThat(thirdRejected.get())
                    .as("Third connection should be rejected by server (max-connections=2)")
                    .isTrue();

        } finally {
            for (WebSocket ws : openSockets) {
                try {
                    ws.close();
                } catch (Exception e) {
                    // ignore cleanup errors
                }
            }
            client.close();
        }
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
}
