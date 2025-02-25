package io.quarkus.websockets.next.test.requestcontext;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
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

public class RequestScopedEndpointTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Echo.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo")
    URI echo;

    @Test
    void testRequestScopedEndpoint() throws InterruptedException {
        try (WSClient client = WSClient.create(vertx).connect(echo)) {
            client.send("foo");
            client.send("bar");
            client.send("baz");
            client.waitForMessages(3);
            assertTrue(Echo.DESTROYED_LATCH.await(5, TimeUnit.SECONDS),
                    "Latch count: " + Echo.DESTROYED_LATCH.getCount());
        }
    }

    @RequestScoped
    @WebSocket(path = "/echo")
    public static class Echo {

        static final CountDownLatch DESTROYED_LATCH = new CountDownLatch(3);

        @OnTextMessage
        String echo(String message) {
            if (!Arc.container().requestContext().isActive()) {
                throw new IllegalStateException();
            }
            return message;
        }

        @PreDestroy
        void destroy() {
            DESTROYED_LATCH.countDown();
        }
    }

}
