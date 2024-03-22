package io.quarkus.websockets.next.test.subprotocol;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;

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
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("endpoint")
    URI endUri;

    @Test
    void testConnectionRejected() {
        CompletionException e = assertThrows(CompletionException.class,
                () -> new WSClient(vertx).connect(new WebSocketConnectOptions().addSubProtocol("oak"), endUri));
        Throwable cause = e.getCause();
        assertTrue(cause instanceof WebSocketClientHandshakeException);
        assertFalse(Endpoint.OPEN_CALLED.get());
    }

    @WebSocket(path = "/endpoint")
    public static class Endpoint {

        static final AtomicBoolean OPEN_CALLED = new AtomicBoolean();

        @OnOpen
        void open() {
            OPEN_CALLED.set(true);
        }

    }

}
