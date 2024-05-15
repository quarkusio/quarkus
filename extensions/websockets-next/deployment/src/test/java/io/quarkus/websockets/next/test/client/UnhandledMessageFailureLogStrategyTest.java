package io.quarkus.websockets.next.test.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnector;

public class UnhandledMessageFailureLogStrategyTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(ServerEndpoint.class, ClientMessageErrorEndpoint.class);
            }).overrideConfigKey("quarkus.websockets-next.client.unhandled-failure-strategy", "log");

    @Inject
    WebSocketConnector<ClientMessageErrorEndpoint> connector;

    @TestHTTPResource("/")
    URI testUri;

    @Test
    void testError() throws InterruptedException {
        WebSocketClientConnection connection = connector
                .baseUri(testUri)
                .connectAndAwait();
        connection.sendTextAndAwait("foo");
        assertFalse(connection.isClosed());
        connection.sendText("bar");
        assertTrue(ClientMessageErrorEndpoint.MESSAGE_LATCH.await(5, TimeUnit.SECONDS));
        assertEquals("bar", ClientMessageErrorEndpoint.MESSAGES.get(0));
    }

}
