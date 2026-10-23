package io.quarkus.websockets.next.test.shutdown;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.shutdown.ShutdownRecorder;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;

/**
 * A WebSocket that was opened and closed again must not hold the graceful shutdown: its upgrade request is over and
 * nothing is in flight anymore.
 */
public class GracefulShutdownAfterWebSocketCloseTest {

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(ShutdownEndpoint.class, WSClient.class))
            .overrideConfigKey("quarkus.shutdown.timeout", "3s");

    @Inject
    Vertx vertx;

    @Inject
    OpenConnections connections;

    @TestHTTPResource("shutdown")
    URI uri;

    @Test
    public void testClosedWebSocketDoesNotHoldGracefulShutdown() {
        try (WSClient client = WSClient.create(vertx).connect(uri)) {
            assertEquals("open", client.waitForNextMessage().toString());
            client.disconnect();
        }
        await().until(() -> connections.listAll().isEmpty());

        List<String> messages = new CopyOnWriteArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                messages.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        List<Logger> loggers = List.of(
                Logger.getLogger("io.quarkus.runtime.shutdown.ShutdownRecorder"),
                Logger.getLogger("io.quarkus.vertx.http.runtime.filters.GracefulShutdownFilter"));
        loggers.forEach(logger -> logger.addHandler(handler));
        long start = System.nanoTime();
        try {
            ShutdownRecorder.runShutdown();
        } finally {
            loggers.forEach(logger -> logger.removeHandler(handler));
        }
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertTrue(!messages.contains("Timed out waiting for graceful shutdown, shutting down anyway."),
                "graceful shutdown timed out, log: " + messages);
        assertTrue(elapsed.compareTo(Duration.ofSeconds(1)) < 0,
                "graceful shutdown waited " + elapsed.toMillis() + " ms although no request was in flight, log: "
                        + messages);
    }

}
