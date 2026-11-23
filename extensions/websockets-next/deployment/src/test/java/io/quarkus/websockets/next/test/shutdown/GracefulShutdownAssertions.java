package io.quarkus.websockets.next.test.shutdown;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import io.quarkus.runtime.shutdown.ShutdownRecorder;

public final class GracefulShutdownAssertions {

    private GracefulShutdownAssertions() {
    }

    /**
     * Runs the real graceful shutdown and asserts that it did not wait for the configured timeout. The application
     * is not usable afterwards, so a test class can call this once.
     */
    public static void assertShutdownDoesNotWait() {
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
