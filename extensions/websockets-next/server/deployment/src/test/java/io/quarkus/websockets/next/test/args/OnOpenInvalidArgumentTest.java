package io.quarkus.websockets.next.test.args;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketServerException;

public class OnOpenInvalidArgumentTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint.class);
            })
            .setExpectedException(WebSocketServerException.class);

    @Test
    void testInvalidArgument() {
        fail();
    }

    @WebSocket(path = "/end")
    public static class Endpoint {

        @OnOpen
        void open(List<String> unsupported) {
        }

    }

}
