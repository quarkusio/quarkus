package io.quarkus.aesh.websocket.deployment;

import java.net.URI;
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
 * Verifies that the WebSocket endpoint closes idle sessions after the configured timeout.
 */
public class AeshWebSocketIdleTimeoutTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(HelloCommand.class))
            .overrideConfigKey("quarkus.aesh.websocket.idle-timeout", "2s");

    @TestHTTPResource("/aesh/terminal")
    URI wsUri;

    @Inject
    Vertx vertx;

    @Test
    public void testIdleSessionIsClosed() throws Exception {
        AtomicBoolean commandWorked = new AtomicBoolean(false);
        CountDownLatch commandLatch = new CountDownLatch(1);
        CountDownLatch closedLatch = new CountDownLatch(1);

        WebSocketClient client = vertx.createWebSocketClient();
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

            ws.textMessageHandler(msg -> {
                if (msg.contains("Hello World!")) {
                    commandWorked.set(true);
                    commandLatch.countDown();
                }
            });

            ws.closeHandler(v -> closedLatch.countDown());

            // Initialize and send a command
            ws.writeTextMessage("{\"action\":\"init\",\"cols\":80,\"rows\":24}");
            vertx.setTimer(500, id -> {
                ws.writeTextMessage("{\"action\":\"read\",\"data\":\"hello\\r\"}");
            });
        });

        // Verify the command works
        boolean cmdOk = commandLatch.await(10, TimeUnit.SECONDS);
        Assertions.assertThat(cmdOk).as("Command should produce output").isTrue();
        Assertions.assertThat(commandWorked.get()).isTrue();

        // Now wait for idle timeout (2s timeout + up to 1s check + buffer)
        boolean closed = closedLatch.await(6, TimeUnit.SECONDS);
        Assertions.assertThat(closed)
                .as("WebSocket should be closed by server after idle timeout")
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
