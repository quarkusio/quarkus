package io.quarkus.websockets.next.test.endpoints;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketException;

public class TooManyOnMessageInSubEndpointTest {

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> {
                root.addClasses(ParentEndpoint.class, ParentEndpoint.SubEndpointWithTooManyOnMessage.class);
            })
            .setExpectedException(WebSocketException.class);

    @Test
    void verifyThatSubEndpointWithoutTooManyOnMessageFailsToDeploy() {

    }

    @WebSocket(path = "/ws")
    public static class ParentEndpoint {

        @OnTextMessage
        public void onMessage(String message) {
            // Ignored.
        }

        @WebSocket(path = "/sub")
        public static class SubEndpointWithTooManyOnMessage {

            @OnTextMessage
            public void onMessage(String message) {
                // Ignored.
            }

            @OnTextMessage
            public void onMessage2(String message) {
                // Ignored.
            }
        }

    }

}
