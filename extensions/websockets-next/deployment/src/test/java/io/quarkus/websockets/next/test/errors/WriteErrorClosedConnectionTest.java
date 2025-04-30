package io.quarkus.websockets.next.test.errors;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

public class WriteErrorClosedConnectionTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Echo.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo")
    URI testUri;

    @Test
    void testError() {
        WSClient client = WSClient.create(vertx).connect(testUri);
        client.sendAndAwait(Buffer.buffer("1"));
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> client.isClosed());
        assertTrue(Echo.ERROR_HANDLER_CALLED.get());
    }

    @WebSocket(path = "/echo")
    public static class Echo {

        static final AtomicBoolean ERROR_HANDLER_CALLED = new AtomicBoolean();

        @OnBinaryMessage
        Uni<Buffer> process(Buffer message, WebSocketConnection connection) {
            // This should result in a failure because the connection is closed
            // but we still try to write a binary message
            return connection.close().replaceWith(message);
        }

        @OnError
        void runtimeProblem(Throwable t, WebSocketConnection connection) {
            if (connection.isOpen()) {
                throw new IllegalStateException();
            }
            ERROR_HANDLER_CALLED.set(true);
        }

    }

}
