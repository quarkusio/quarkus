package io.quarkus.websockets.next.test.endpoints;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketServerException;

public class EmptyEndpointTest {

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> {
                root.addClasses(EmptyEndpoint.class);
            })
            .setExpectedException(WebSocketServerException.class);

    @Test
    void verifyThatEndpointWithoutAnyMethodFailsToDeploy() {
        fail();
    }

    @WebSocket(path = "/ws")
    public static class EmptyEndpoint {

    }

}
