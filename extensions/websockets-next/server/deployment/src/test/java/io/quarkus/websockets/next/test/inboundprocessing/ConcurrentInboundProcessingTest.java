package io.quarkus.websockets.next.test.inboundprocessing;

import static io.quarkus.websockets.next.InboundProcessingMode.CONCURRENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;

public class ConcurrentInboundProcessingTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Sim.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("sim")
    URI simUri;

    @Test
    void testSimultaneousExecution() {
        WSClient client = WSClient.create(vertx).connect(simUri);
        client.send("1");
        client.send("2");
        client.send("3");
        client.send("4");
        client.waitForMessages(4);
        for (int i = 0; i < 4; i++) {
            assertEquals("ok", client.getMessages().get(i).toString());
        }
    }

    @WebSocket(path = "/sim", inboundProcessingMode = CONCURRENT)
    public static class Sim {

        private final CountDownLatch latch = new CountDownLatch(4);

        @OnTextMessage
        String process(String message) throws InterruptedException {
            latch.countDown();
            // Now wait for other messages to arrive
            if (latch.await(10, TimeUnit.SECONDS)) {
                return "ok";
            } else {
                return "" + latch.getCount();
            }
        }

    }

}
