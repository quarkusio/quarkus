package io.quarkus.websockets.next.test.inboundprocessing;

import static io.quarkus.websockets.next.InboundProcessingMode.CONCURRENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Vertx;

public class MaxPendingMessagesTest {

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> {
                root.addClasses(Limited.class, LimitedMulti.class, WSClient.class);
            })
            // .overrideConfigKey("quarkus.log.category.\"io.quarkus.websockets.next.runtime.Endpoints\".level", "DEBUG")
            .overrideConfigKey("quarkus.websockets-next.server.max-pending-messages", "2");

    @Inject
    Vertx vertx;

    @TestHTTPResource("limited")
    URI limitedUri;

    @TestHTTPResource("limited-multi")
    URI limitedMultiUri;

    @Test
    void testInFlightMessagesAreBounded() throws InterruptedException {
        WSClient client = WSClient.create(vertx).connect(limitedUri);
        // Send more messages than the configured limit
        client.send("1");
        client.send("2");
        client.send("3");
        client.send("4");

        // Exactly two messages are allowed to be processed concurrently
        assertTrue(Limited.twoStarted.await(5, TimeUnit.SECONDS), "Two messages should be processed concurrently");
        // Give a potential (incorrectly unbounded) third message a chance to start before we release the gate
        Thread.sleep(500);
        assertEquals(2, Limited.inFlight.get(), "No more than max-pending-messages should be in flight");
        assertEquals(2, Limited.peak.get(), "The peak number of in-flight messages must not exceed the limit");

        // Release the held messages - the remaining messages are fetched one at a time as the in-flight ones complete
        Limited.releaseGate.countDown();
        client.waitForMessages(4);
        for (int i = 0; i < 4; i++) {
            assertEquals("ok", client.getMessages().get(i).toString());
        }
    }

    @Test
    void testMultiStreamOfMessages() {
        WSClient client = WSClient.create(vertx).connect(limitedMultiUri);
        // Stream more messages than the configured limit; fetch(1) on each downstream emission must keep the
        // stream flowing past the initial fetch(max-pending-messages) without stalling or dropping messages
        int count = 10;
        for (int i = 0; i < count; i++) {
            client.send("m" + i);
        }
        client.waitForMessages(count);
        for (int i = 0; i < count; i++) {
            assertEquals("echo-m" + i, client.getMessages().get(i).toString());
        }
    }

    @WebSocket(path = "/limited", inboundProcessingMode = CONCURRENT)
    public static class Limited {

        static final AtomicInteger inFlight = new AtomicInteger();
        static final AtomicInteger peak = new AtomicInteger();
        static final CountDownLatch twoStarted = new CountDownLatch(2);
        static final CountDownLatch releaseGate = new CountDownLatch(1);

        @OnTextMessage
        String process(String message) throws InterruptedException {
            int current = inFlight.incrementAndGet();
            peak.accumulateAndGet(current, Math::max);
            twoStarted.countDown();
            try {
                releaseGate.await(10, TimeUnit.SECONDS);
                return "ok";
            } finally {
                inFlight.decrementAndGet();
            }
        }

    }

    @WebSocket(path = "/limited-multi")
    public static class LimitedMulti {

        @OnTextMessage
        Multi<String> process(Multi<String> messages) {
            return messages.map(m -> "echo-" + m);
        }

    }

}
