package io.quarkus.websockets.next.test.endpoints;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketServerException;

public class InvalidPathParamTest {

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> {
                root.addClasses(InvalidPathParam.class);
            })
            .setExpectedException(WebSocketServerException.class);

    @Test
    void testInvalidPathParam() {
        fail();
    }

    @WebSocket(path = "/service/{invalid}_{param}")
    public static class InvalidPathParam {

        @OnOpen
        public void onOpen() {
        }

    }

}
