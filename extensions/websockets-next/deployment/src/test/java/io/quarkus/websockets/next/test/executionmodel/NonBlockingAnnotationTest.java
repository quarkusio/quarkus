package io.quarkus.websockets.next.test.executionmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.common.annotation.NonBlocking;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class NonBlockingAnnotationTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("endpoint")
    URI endUri;

    @Test
    void testEndoint() {
        try (WSClient client = new WSClient(vertx)) {
            client.connect(endUri);
            assertEquals("evenloop:true,worker:false", client.sendAndAwaitReply("foo").toString());
        }
    }

    @WebSocket(path = "/endpoint")
    public static class Endpoint {

        @NonBlocking
        @OnTextMessage
        String message(String ignored) {
            return "evenloop:" + Context.isOnEventLoopThread() + ",worker:" + Context.isOnWorkerThread();
        }

    }

}
