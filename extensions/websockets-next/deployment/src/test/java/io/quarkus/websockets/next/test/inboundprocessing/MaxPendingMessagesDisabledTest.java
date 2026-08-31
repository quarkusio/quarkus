package io.quarkus.websockets.next.test.inboundprocessing;

import static io.quarkus.websockets.next.InboundProcessingMode.CONCURRENT;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.Vertx;

public class MaxPendingMessagesDisabledTest {

    // Greater than the default quarkus.websockets-next.server.max-pending-messages (256)
    static final int COUNT = 300;

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Unbounded.class, WSClient.class);
            }).overrideConfigKey("quarkus.websockets-next.server.max-pending-messages", "0");

    @Inject
    Vertx vertx;

    @TestHTTPResource("unbounded")
    URI unboundedUri;

    @Test
    void testBackpressureDisabled() {
        WSClient client = WSClient.create(vertx).connect(unboundedUri);
        // Send more messages than the default limit; with back-pressure disabled all of them are received and
        // parked in flight at once - if any bound was applied, in-flight would cap below COUNT and this would time out
        for (int i = 0; i < COUNT; i++) {
            client.send("m" + i);
        }
        Awaitility.await().until(() -> Unbounded.inFlight.get() == COUNT);
        // Release all parked messages and make sure they are all processed
        Unbounded.releaseAll();
        client.waitForMessages(COUNT);
    }

    @WebSocket(path = "/unbounded", inboundProcessingMode = CONCURRENT)
    public static class Unbounded {

        static final AtomicInteger inFlight = new AtomicInteger();
        static final List<UniEmitter<? super String>> emitters = new CopyOnWriteArrayList<>();

        static void releaseAll() {
            for (UniEmitter<? super String> emitter : emitters) {
                emitter.complete("ok");
            }
        }

        // Non-blocking handler - each message is parked via an emitter (no thread consumed) so the event loop keeps
        // reading and unprocessed messages accumulate when back-pressure is disabled
        @OnTextMessage
        Uni<String> process(String message) {
            return Uni.createFrom().emitter(emitter -> {
                emitters.add(emitter);
                inFlight.incrementAndGet();
            });
        }

    }

}
