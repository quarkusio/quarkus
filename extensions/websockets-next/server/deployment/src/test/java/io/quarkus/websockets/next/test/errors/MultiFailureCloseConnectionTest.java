package io.quarkus.websockets.next.test.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.time.Duration;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public class MultiFailureCloseConnectionTest {

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
        client.sendAndAwait("bar,foo,baz");
        // "bar" should be sent back
        client.waitForMessages(1);
        // "foo" results in a failure -> connection closed
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> client.isClosed());
        // "foo" and "baz" should never be sent back
        assertEquals(1, client.getMessages().size());
    }

    @WebSocket(path = "/echo")
    public static class Echo {

        @OnTextMessage
        Multi<String> process(String message) {
            return Multi.createFrom().items(message.split(",")).invoke(s -> {
                if (s.equals("foo")) {
                    throw new IllegalArgumentException();
                }
            });
        }

        @OnError
        Uni<Void> runtimeProblem(IllegalArgumentException e, WebSocketConnection connection) {
            return connection.close();
        }

    }

}
