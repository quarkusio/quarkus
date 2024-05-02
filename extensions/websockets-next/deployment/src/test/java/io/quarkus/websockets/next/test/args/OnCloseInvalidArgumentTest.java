package io.quarkus.websockets.next.test.args;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketException;

public class OnCloseInvalidArgumentTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint.class);
            })
            .setExpectedException(WebSocketException.class);

    @Test
    void testInvalidArgument() {
        fail();
    }

    @WebSocket(path = "/end")
    public static class Endpoint {

        @OnClose
        void close(List<String> unsupported) {
        }

    }

}
