package io.quarkus.websockets.next.test.endpoints;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketException;

public class TooManyOnOpenInSubEndpointTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(ParentEndpoint.class, ParentEndpoint.SubEndpointWithTooManyOnOpen.class);
            })
            .setExpectedException(WebSocketException.class);

    @Test
    void verifyThatSubEndpointWithoutTooManyOnOpenFailsToDeploy() {

    }

    @WebSocket(path = "/ws")
    public static class ParentEndpoint {

        @OnTextMessage
        public void onMessage(String message) {
            // Ignored.
        }

        @WebSocket(path = "/sub")
        public static class SubEndpointWithTooManyOnOpen {

            @OnTextMessage
            public void onMessage(String message) {
                // Ignored.
            }

            @OnOpen
            public void onOpen() {
                // Ignored.
            }

            @OnOpen
            public void onOpen2() {
                // Ignored.
            }

        }

    }

}
