package io.quarkus.websockets.next.test.endpoints;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketException;

public class TooManyOnMessageTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(TooManyOnMessage.class);
            })
            .setExpectedException(WebSocketException.class);

    @Test
    void verifyThatEndpointWithMultipleOnMessageMethodsFailsToDeploy() {

    }

    @WebSocket(path = "/ws")
    public static class TooManyOnMessage {
        @OnTextMessage
        public void onMessage(String message) {
        }

        @OnTextMessage
        public void onMessage2(String message) {
        }
    }

}
