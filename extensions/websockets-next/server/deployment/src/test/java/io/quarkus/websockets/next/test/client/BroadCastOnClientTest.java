package io.quarkus.websockets.next.test.client;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientException;

public class BroadCastOnClientTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(ServerEndpoint.class, ClientEndpoint.class);
            })
            .setExpectedException(WebSocketClientException.class, true);

    @Test
    void testInvalidBroadcast() {
        fail();
    }

    @WebSocket(path = "/end")
    public static class ServerEndpoint {

        @OnOpen
        void open() {
        }

    }

    @WebSocketClient(path = "/end")
    public static class ClientEndpoint {

        @OnTextMessage(broadcast = true)
        String echo(String message) {
            return message;
        }

    }
}
