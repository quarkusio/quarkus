package io.quarkus.websockets.next.test.errors;

import java.net.URI;
import java.time.Duration;

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

public class RuntimeErrorCloseConnectionTest {

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
    }

    @WebSocket(path = "/echo")
    public static class Echo {

        @OnBinaryMessage
        void process(Buffer message) {
            throw new IllegalStateException("Something went wrong");
        }

        @OnError
        Uni<Void> runtimeProblem(RuntimeException e, WebSocketConnection connection) {
            return connection.close();
        }

    }

}
