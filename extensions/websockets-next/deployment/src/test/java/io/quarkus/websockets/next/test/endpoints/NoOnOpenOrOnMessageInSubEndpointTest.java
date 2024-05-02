package io.quarkus.websockets.next.test.endpoints;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketServerException;

public class NoOnOpenOrOnMessageInSubEndpointTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(ParentEndpoint.class, ParentEndpoint.SubEndpointWithoutOnOpenAndOnMessage.class);
            })
            .setExpectedException(WebSocketServerException.class);

    @Test
    void verifyThatSubEndpointWithoutOnOpenOrOnMessageFailsToDeploy() {

    }

    @WebSocket(path = "/ws")
    public static class ParentEndpoint {

        @OnTextMessage
        public void onMessage(String message) {
            // Ignored.
        }

        @WebSocket(path = "/sub")
        public static class SubEndpointWithoutOnOpenAndOnMessage {

            @OnClose
            public void onClose() {
                // Ignored.
            }

        }

    }

}
