package io.quarkus.websockets.next.test.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnector;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.WebSocketBase;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.http.WebSocketFrame;

public class FragmentedMessagesBackpressureTest {

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Client.class, Server.class))
            .overrideRuntimeConfigKey("quarkus.websockets-next.client.max-pending-messages", "1")
            .overrideRuntimeConfigKey("quarkus.websockets-next.server.max-pending-messages", "1");

    @Inject
    Vertx vertx;

    @Inject
    WebSocketConnector<Client> connector;

    @TestHTTPResource("/fragmented")
    URI uri;

    @Test
    void basicClientText() throws Exception {
        basicClient(false);
    }

    @Test
    void basicClientBinary() throws Exception {
        basicClient(true);
    }

    private void basicClient(boolean binary) throws Exception {
        BasicWebSocketConnector basicConnector = BasicWebSocketConnector.create();
        HttpServer server = sendingServer(binary);
        CountDownLatch received = new CountDownLatch(10);
        CountDownLatch pings = new CountDownLatch(10);
        try {
            basicConnector.baseUri("ws://localhost:" + server.actualPort()).onPing((c, m) -> pings.countDown());
            if (binary) {
                basicConnector.onBinaryMessage((c, m) -> {
                    assertThat(m.toString()).isEqualTo("hello");
                    received.countDown();
                });
            } else {
                basicConnector.onTextMessage((c, m) -> {
                    assertThat(m).isEqualTo("hello");
                    received.countDown();
                });
            }
            WebSocketClientConnection connection = basicConnector.connectAndAwait();
            try {
                assertThat(received.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(pings.await(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                connection.closeAndAwait();
            }
        } finally {
            server.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void basicClientIgnoresUnconsumedMessages() throws Exception {
        BasicWebSocketConnector basicConnector = BasicWebSocketConnector.create();
        HttpServer server = vertx.createHttpServer().webSocketHandler(ws -> {
            sendFragments(ws, true);
            sendFragments(ws, false);
        })
                .listen(0)
                .toCompletionStage()
                .toCompletableFuture()
                .get(5, TimeUnit.SECONDS);
        CountDownLatch received = new CountDownLatch(10);
        try {
            WebSocketClientConnection connection = basicConnector.baseUri("ws://localhost:" + server.actualPort())
                    .onTextMessage((c, m) -> received.countDown()).connectAndAwait();
            try {
                assertThat(received.await(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                connection.closeAndAwait();
            }
        } finally {
            server.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void annotatedClientText() throws Exception {
        annotatedClient(false);
    }

    @Test
    void annotatedClientBinary() throws Exception {
        annotatedClient(true);
    }

    private void annotatedClient(boolean binary) throws Exception {
        Client.received = new CountDownLatch(10);
        HttpServer server = sendingServer(binary);
        try {
            WebSocketClientConnection connection = connector.baseUri("ws://localhost:" + server.actualPort())
                    .connectAndAwait();
            try {
                assertThat(Client.received.await(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                connection.closeAndAwait();
            }
        } finally {
            server.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void annotatedServerText() throws Exception {
        annotatedServer(false);
    }

    @Test
    void annotatedServerBinary() throws Exception {
        annotatedServer(true);
    }

    private void annotatedServer(boolean binary) throws Exception {
        Server.received = new CountDownLatch(10);
        var client = vertx.createWebSocketClient();
        try {
            var ws = client.connect(new WebSocketConnectOptions().setHost(uri.getHost())
                    .setPort(uri.getPort())
                    .setURI(uri.getPath()))
                    .toCompletionStage()
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);
            sendFragments(ws, binary);
            assertThat(Server.received.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }

    private HttpServer sendingServer(boolean binary) throws Exception {
        return vertx.createHttpServer()
                .webSocketHandler(ws -> sendFragments(ws, binary))
                .listen(0)
                .toCompletionStage()
                .toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    private static void sendFragments(WebSocketBase ws, boolean binary) {
        for (int i = 0; i < 10; i++) {
            // Even a small message can have more fragments than the pending-message limit
            ws.writeFrame(binary ? WebSocketFrame.binaryFrame(Buffer.buffer("he"), false)
                    : WebSocketFrame.textFrame("he", false));
            ws.writePing(Buffer.buffer("ping"));
            ws.writeFrame(WebSocketFrame.continuationFrame(Buffer.buffer("ll"), false));
            ws.writeFrame(WebSocketFrame.continuationFrame(Buffer.buffer("o"), true));
        }
    }

    @WebSocketClient(path = "/")
    public static class Client {

        static volatile CountDownLatch received;

        @OnTextMessage
        void text(String message) {
            if (!"hello".equals(message)) {
                throw new IllegalStateException();
            }
            received.countDown();
        }

        @OnBinaryMessage
        void binary(Buffer message) {
            text(message.toString());
        }
    }

    @WebSocket(path = "/fragmented")
    public static class Server {

        static volatile CountDownLatch received;

        @OnTextMessage
        void text(String message) {
            if (!"hello".equals(message)) {
                throw new IllegalStateException();
            }
            received.countDown();
        }

        @OnBinaryMessage
        void binary(Buffer message) {
            text(message.toString());
        }
    }
}
