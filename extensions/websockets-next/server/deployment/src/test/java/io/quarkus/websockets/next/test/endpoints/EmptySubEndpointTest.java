package io.quarkus.websockets.next.test.endpoints;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketServerException;

public class EmptySubEndpointTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(ParentEndpoint.class, ParentEndpoint.EmptySubEndpoint.class);
            })
            .setExpectedException(WebSocketServerException.class);

    @Test
    void verifyThatSubEndpointWithoutAnyMethodFailsToDeploy() {

    }

    @WebSocket("/ws")
    public static class ParentEndpoint {

        @OnMessage
        public void onMessage(String message) {
            // Ignored.
        }

        @WebSocket("/sub")
        public static class EmptySubEndpoint {

        }

    }

}
