package io.quarkus.websockets.next.test.endpoints;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketServerException;

public class AmbiguousEndpointIdTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint1.class, Endpoint2.class);
            })
            .setExpectedException(WebSocketServerException.class);

    @Test
    public void testEndpointIds() {
        fail();
    }

    @WebSocket(path = "/ws1", endpointId = "foo")
    public static class Endpoint1 {

        @OnOpen
        void open() {
        }

    }

    @WebSocket(path = "/ws2", endpointId = "foo")
    public static class Endpoint2 {

        @OnOpen
        void open() {
        }

    }

}
