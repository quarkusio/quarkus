package io.quarkus.websockets.next.test.errors;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketException;

public class MultipleAmbiguousErrorHandlersTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint.class);
            })
            .setExpectedException(WebSocketException.class);

    @Test
    void testMultipleAmbiguousErrorHandlers() {
        fail();
    }

    @WebSocket(path = "/end")
    public static class Endpoint {

        @OnOpen
        void open() {
        }

        @OnError
        void onError1(IllegalStateException ise) {
        }

        @OnError
        void onError2(IllegalStateException ise) {
        }

    }

}
