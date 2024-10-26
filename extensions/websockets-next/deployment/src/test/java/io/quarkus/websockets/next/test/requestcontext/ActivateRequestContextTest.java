package io.quarkus.websockets.next.test.requestcontext;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;

public class ActivateRequestContextTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("end")
    URI endUri;

    @Test
    void testRequestContext() throws InterruptedException {
        try (WSClient client = WSClient.create(vertx).connect(endUri)) {
            client.sendAndAwait("ping");
            client.waitForMessages(1);
            assertEquals("pong:true", client.getLastMessage().toString());
        }
    }

    @WebSocket(path = "/end")
    public static class Endpoint {

        @ActivateRequestContext // the request context should not be activated by WS next
        @OnTextMessage
        String process(String message) {
            return "pong:" + Arc.container().requestContext().isActive();
        }
    }

}
