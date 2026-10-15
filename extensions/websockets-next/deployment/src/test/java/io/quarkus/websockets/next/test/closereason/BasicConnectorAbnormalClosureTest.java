package io.quarkus.websockets.next.test.closereason;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.Closed;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.NetSocket;

class BasicConnectorAbnormalClosureTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(ClosedConnections.class))
            .overrideRuntimeConfigKey("quarkus.websockets-next.client.connection-closing-timeout", "1s");

    @Inject
    Vertx vertx;

    @Test
    void testClosingHandshakeTimeout() throws Exception {
        HttpServer server = startServer(new CompletableFuture<>());
        try (AutoCloseable ignored = () -> server.close().toCompletionStage().toCompletableFuture().get()) {
            CompletableFuture<CloseReason> closeReason = new CompletableFuture<>();
            WebSocketClientConnection connection = connect(server, closeReason);
            connection.closeAndAwait();
            assertAbnormalClosure(connection, closeReason);
        }
    }

    @Test
    void testConnectionLostWithoutCloseFrame() throws Exception {
        CompletableFuture<NetSocket> serverSocket = new CompletableFuture<>();
        HttpServer server = startServer(serverSocket);
        try (AutoCloseable ignored = () -> server.close().toCompletionStage().toCompletableFuture().get()) {
            CompletableFuture<CloseReason> closeReason = new CompletableFuture<>();
            WebSocketClientConnection connection = connect(server, closeReason);
            serverSocket.get().close();
            assertAbnormalClosure(connection, closeReason);
        }
    }

    private HttpServer startServer(CompletableFuture<NetSocket> serverSocket) throws TimeoutException {
        return vertx.createHttpServer()
                .requestHandler(request -> {
                    request.response().headers()
                            .set("Upgrade", "websocket")
                            .set("Connection", "Upgrade")
                            .set("Sec-WebSocket-Accept", acceptKey(request.getHeader("Sec-WebSocket-Key")));
                    request.toNetSocket().onSuccess(serverSocket::complete);
                })
                .listen(0)
                .await(5, TimeUnit.SECONDS);
    }

    private static WebSocketClientConnection connect(HttpServer server, CompletableFuture<CloseReason> closeReason) {
        return BasicWebSocketConnector.create()
                .baseUri("ws://localhost:" + server.actualPort())
                .onClose((c, reason) -> closeReason.complete(reason))
                .connectAndAwait();
    }

    private static void assertAbnormalClosure(WebSocketClientConnection connection,
            CompletableFuture<CloseReason> closeReason) {
        assertThat(closeReason).succeedsWithin(Duration.ofSeconds(5))
                .extracting(CloseReason::getCode)
                .isEqualTo(CloseReason.ABNORMAL.getCode());
        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(ClosedConnections.IDS).contains(connection.id()));
    }

    private static String acceptKey(String key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest((key + WebSocketClientHandshaker13.MAGIC_GUID).getBytes(StandardCharsets.US_ASCII));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Singleton
    static class ClosedConnections {

        static final Set<String> IDS = ConcurrentHashMap.newKeySet();

        void onClose(@ObservesAsync @Closed WebSocketClientConnection connection) {
            IDS.add(connection.id());
        }
    }

}
