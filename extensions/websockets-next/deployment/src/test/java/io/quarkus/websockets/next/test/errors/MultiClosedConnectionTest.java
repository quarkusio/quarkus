package io.quarkus.websockets.next.test.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.logging.Log;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Vertx;
import io.vertx.core.impl.NoStackTraceThrowable;

public class MultiClosedConnectionTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Echo.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo")
    URI testUri;

    @Test
    void testError() throws InterruptedException {
        WSClient client = WSClient.create(vertx).connect(testUri);
        client.waitForMessages(1);
        client.close();
        assertTrue(Echo.TERMINATION_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(Echo.ERROR_LATCH.await(5, TimeUnit.SECONDS));
        // Connection is closed and the returned Multi should be cancelled
        int numOfMessages = Echo.MESSAGES.size();
        Thread.sleep(600);
        // No more ticks are emitted
        assertEquals(numOfMessages, Echo.MESSAGES.size());
    }

    @WebSocket(path = "/echo")
    public static class Echo {

        static final CountDownLatch TERMINATION_LATCH = new CountDownLatch(1);
        static final CountDownLatch ERROR_LATCH = new CountDownLatch(1);

        static final List<String> MESSAGES = new CopyOnWriteArrayList<>();

        @OnOpen
        Multi<String> onOpen() {
            return Multi.createFrom()
                    .ticks()
                    .every(Duration.ofMillis(300))
                    .map(tick -> tick + "")
                    .invoke(s -> {
                        Log.infof("Next tick: %s", s);
                        MESSAGES.add(s);
                    })
                    .onTermination()
                    .invoke(() -> {
                        Log.info("Terminated!");
                        TERMINATION_LATCH.countDown();
                    });
        }

        @OnError
        void onConnectionClosedError(NoStackTraceThrowable t, WebSocketConnection conn) {
            Log.info("Error callback!");
            if (conn.isClosed()) {
                String message = t.getMessage();
                if (message != null && message.contains("WebSocket is closed")) {
                    ERROR_LATCH.countDown();
                }
            }
        }

    }

}
