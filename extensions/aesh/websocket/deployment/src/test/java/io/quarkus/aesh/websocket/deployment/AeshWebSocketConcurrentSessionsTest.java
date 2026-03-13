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
 * Verifies that multiple WebSocket clients can connect simultaneously to
 * the {@code /aesh/terminal} endpoint and each receives correct command output.
 */
public class AeshWebSocketConcurrentSessionsTest {

    private static final int SESSION_COUNT = 3;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(HelloCommand.class));

    @TestHTTPResource("/aesh/terminal")
    URI wsUri;

    @Inject
    Vertx vertx;

    @Test
    public void testConcurrentWebSocketSessions() throws Exception {
        CopyOnWriteArrayList<String> results = new CopyOnWriteArrayList<>();
        CountDownLatch allDone = new CountDownLatch(SESSION_COUNT);

        for (int i = 0; i < SESSION_COUNT; i++) {
            WebSocketClient client = vertx.createWebSocketClient();
            WebSocketConnectOptions options = new WebSocketConnectOptions()
                    .setHost(wsUri.getHost())
                    .setPort(wsUri.getPort())
                    .setURI(wsUri.getPath());

            client.connect(options).onComplete(ar -> {
                if (ar.failed()) {
                    results.add("CONNECT_FAILED: " + ar.cause().getMessage());
                    allDone.countDown();
                    return;
                }
                var ws = ar.result();

                ws.textMessageHandler(msg -> {
                    if (msg.contains("Hello World!")) {
                        results.add(msg);
                        allDone.countDown();
                    }
                });

                // Initialize terminal then send command
                ws.writeTextMessage("{\"action\":\"init\",\"cols\":80,\"rows\":24}");
                vertx.setTimer(500, id -> {
                    ws.writeTextMessage("{\"action\":\"read\",\"data\":\"hello\\r\"}");
                });
            });
        }

        boolean completed = allDone.await(30, TimeUnit.SECONDS);
        Assertions.assertThat(completed)
                .as("All %d WebSocket sessions should complete within 30s. Got %d results: %s",
                        SESSION_COUNT, results.size(), results)
                .isTrue();
        Assertions.assertThat(results).hasSize(SESSION_COUNT);
        for (String result : results) {
            Assertions.assertThat(result)
                    .doesNotStartWith("CONNECT_FAILED")
                    .contains("Hello World!");
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
