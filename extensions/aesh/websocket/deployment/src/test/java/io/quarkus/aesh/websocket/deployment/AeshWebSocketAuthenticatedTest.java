package io.quarkus.aesh.websocket.deployment;

import java.net.URI;
import java.util.Base64;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;

/**
 * Tests that the WebSocket terminal requires authentication when
 * {@code quarkus.aesh.websocket.authenticated=true} is set.
 */
public class AeshWebSocketAuthenticatedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    HelloCommand.class,
                    GoodbyeCommand.class,
                    TestIdentityProvider.class,
                    TestIdentityController.class))
            .overrideConfigKey("quarkus.aesh.websocket.authenticated", "true");

    @TestHTTPResource("/aesh/terminal")
    URI wsUri;

    @Inject
    Vertx vertx;

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("user", "user", "user");
    }

    @Test
    public void testUnauthenticatedConnectionRejected() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<>();

        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setHost(wsUri.getHost())
                .setPort(wsUri.getPort())
                .setURI(wsUri.getPath());

        client.connect(options).onComplete(ar -> {
            if (ar.failed()) {
                errors.add(ar.cause());
            }
            latch.countDown();
        });

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        Assertions.assertThat(completed).isTrue();
        Assertions.assertThat(errors).hasSize(1);
        Assertions.assertThat(errors.get(0).getMessage()).contains("401");
    }

    @Test
    public void testAuthenticatedConnectionSucceeds() throws Exception {
        CopyOnWriteArrayList<String> messages = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        WebSocketClient client = vertx.createWebSocketClient();
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setHost(wsUri.getHost())
                .setPort(wsUri.getPort())
                .setURI(wsUri.getPath())
                .addHeader("Authorization", basicAuth("user", "user"));

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

            ws.writeTextMessage("{\"action\":\"init\",\"cols\":80,\"rows\":24}");

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

    private static String basicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
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
