package io.quarkus.websockets.next.test.subprotocol;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakeException;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketConnectOptions;

public class SubprotocolNotAvailableTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint.class, WSClient.class);
            }).overrideConfigKey("quarkus.websockets-next.server.activate-session-context", "always");

    @Inject
    Vertx vertx;

    @TestHTTPResource("endpoint")
    URI endUri;

    @Test
    void testConnectionRejected() {
        CompletionException e = assertThrows(CompletionException.class,
                () -> {
                    try (WSClient connect = new WSClient(vertx).connect(new WebSocketConnectOptions().addSubProtocol("oak"),
                            endUri)) {
                        // handshake should fail
                    }
                });
        Throwable cause = e.getCause();
        assertTrue(cause instanceof WebSocketClientHandshakeException);
        assertFalse(Endpoint.OPEN_CALLED.get());
        // Wait until the CDI singleton context is destroyed
        // Otherwise the test app is shut down before the WebSocketSessionContext is ended properly
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> Endpoint.SESSION_CONTEXT_DESTROYED.get());
    }

    @WebSocket(path = "/endpoint")
    public static class Endpoint {

        static final AtomicBoolean OPEN_CALLED = new AtomicBoolean();
        static final AtomicBoolean SESSION_CONTEXT_DESTROYED = new AtomicBoolean();

        @OnOpen
        void open() {
            OPEN_CALLED.set(true);
        }

        static void sessionContextDestroyed(@Observes @Destroyed(SessionScoped.class) Object event) {
            SESSION_CONTEXT_DESTROYED.set(true);
        }

    }

}
