package io.quarkus.websockets.next.test.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnector;

public class UnhandledOpenFailureDefaultStrategyTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(ServerEndpoint.class, ClientOpenErrorEndpoint.class);
            });

    @Inject
    WebSocketConnector<ClientOpenErrorEndpoint> connector;

    @TestHTTPResource("/")
    URI testUri;

    @Test
    void testError() throws InterruptedException {
        WebSocketClientConnection connection = connector
                .baseUri(testUri)
                .connectAndAwait();
        assertTrue(ServerEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(ClientOpenErrorEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(connection.isClosed());
        assertEquals(WebSocketCloseStatus.INTERNAL_SERVER_ERROR.code(), connection.closeReason().getCode());
        assertTrue(ClientOpenErrorEndpoint.MESSAGES.isEmpty());
    }

}
