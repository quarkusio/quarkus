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
 * Verifies that the WebSocket endpoint closes idle sessions after the configured timeout.
 */
public class AeshWebSocketIdleTimeoutTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    AeshWebSocketTestHelper.class, HelloCommand.class))
            .overrideConfigKey("quarkus.aesh.websocket.idle-timeout", "2s");

    @TestHTTPResource("/aesh/terminal")
    URI wsUri;

    @Inject
    Vertx vertx;

    @Test
    public void testIdleSessionIsClosed() throws Exception {
        CountDownLatch commandLatch = new CountDownLatch(1);
        CountDownLatch closedLatch = new CountDownLatch(1);
        StringBuilder output = new StringBuilder();

        WebSocketClient client = vertx.createWebSocketClient();
        try {
            WebSocketConnectOptions options = new WebSocketConnectOptions()
                    .setHost(wsUri.getHost())
                    .setPort(wsUri.getPort())
                    .setURI(wsUri.getPath());

            client.connect(options).onComplete(ar -> {
                if (ar.failed()) {
                    commandLatch.countDown();
                    closedLatch.countDown();
                    return;
                }
                var ws = ar.result();

                AeshWebSocketTestHelper.sendCommandOnPrompt(ws, "hello", "Hello World!",
                        output, commandLatch);

                ws.closeHandler(v -> closedLatch.countDown());

                ws.writeTextMessage("{\"action\":\"init\",\"cols\":80,\"rows\":24}");
            });

            // Verify the command works
            boolean cmdOk = commandLatch.await(30, TimeUnit.SECONDS);
            Assertions.assertThat(cmdOk).as("Command should produce output").isTrue();
            Assertions.assertThat(output.toString()).contains("Hello World!");

            // Now wait for idle timeout (2s timeout + up to 1s check + buffer)
            boolean closed = closedLatch.await(6, TimeUnit.SECONDS);
            Assertions.assertThat(closed)
                    .as("WebSocket should be closed by server after idle timeout")
                    .isTrue();
        } finally {
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
