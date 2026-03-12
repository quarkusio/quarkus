package io.quarkus.websockets.next.test.endpoints;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketServerException;

public class EmptySubEndpointTest {

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> {
                root.addClasses(ParentEndpoint.class, ParentEndpoint.EmptySubEndpoint.class);
            })
            .setExpectedException(WebSocketServerException.class);

    @Test
    void verifyThatSubEndpointWithoutAnyMethodFailsToDeploy() {

    }

    @WebSocket(path = "/ws")
    public static class ParentEndpoint {

        @OnTextMessage
        public void onMessage(String message) {
            // Ignored.
        }

        @WebSocket(path = "/sub")
        public static class EmptySubEndpoint {

        }

    }

}
