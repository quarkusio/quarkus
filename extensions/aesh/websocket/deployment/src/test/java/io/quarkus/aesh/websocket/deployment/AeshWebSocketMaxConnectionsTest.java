package io.quarkus.aesh.websocket.deployment;

import java.net.URI;
import java.util.concurrent.CopyOnWriteArrayList;
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

import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

/**
 * Verifies that the WebSocket endpoint enforces the max-connections limit.
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
        CopyOnWriteArrayList<String> results = new CopyOnWriteArrayList<>();
        CountDownLatch twoConnected = new CountDownLatch(2);
        AtomicBoolean thirdClosed = new AtomicBoolean(false);
        CountDownLatch thirdDone = new CountDownLatch(1);

        // Open two connections that should succeed
        for (int i = 0; i < 2; i++) {
            WebSocketClient client = vertx.createWebSocketClient();
            WebSocketConnectOptions options = new WebSocketConnectOptions()
                    .setHost(wsUri.getHost())
                    .setPort(wsUri.getPort())
                    .setURI(wsUri.getPath());

            client.connect(options).onComplete(ar -> {
                if (ar.failed()) {
                    results.add("CONNECT_FAILED");
                    twoConnected.countDown();
                    return;
                }
                var ws = ar.result();
                ws.textMessageHandler(msg -> {
                    if (msg.contains("Hello World!")) {
                        results.add(msg);
                        twoConnected.countDown();
                    }
                });
                ws.writeTextMessage("{\"action\":\"init\",\"cols\":80,\"rows\":24}");
                vertx.setTimer(500, id -> {
                    ws.writeTextMessage("{\"action\":\"read\",\"data\":\"hello\\r\"}");
                });
            });
        }

        boolean twoOk = twoConnected.await(15, TimeUnit.SECONDS);
        Assertions.assertThat(twoOk)
                .as("First two connections should succeed. Got: %s", results)
                .isTrue();
        Assertions.assertThat(results).hasSize(2);

        // Third connection should be rejected
        WebSocketClient client3 = vertx.createWebSocketClient();
        WebSocketConnectOptions options3 = new WebSocketConnectOptions()
                .setHost(wsUri.getHost())
                .setPort(wsUri.getPort())
                .setURI(wsUri.getPath());

        client3.connect(options3).onComplete(ar -> {
            if (ar.failed()) {
                thirdClosed.set(true);
                thirdDone.countDown();
                return;
            }
            var ws = ar.result();
            ws.closeHandler(v -> {
                thirdClosed.set(true);
                thirdDone.countDown();
            });
            // Send init to trigger the max-connections check
            ws.writeTextMessage("{\"action\":\"init\",\"cols\":80,\"rows\":24}");
        });

        boolean done = thirdDone.await(10, TimeUnit.SECONDS);
        Assertions.assertThat(done).as("Third connection attempt should complete").isTrue();
        Assertions.assertThat(thirdClosed.get())
                .as("Third connection should be closed by server")
                .isTrue();
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
