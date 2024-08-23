package io.quarkus.vertx.http.runtime;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * An interface extending the metrics exposed for the Vert.x HTTP server.
 * <p>
 * The Vert.x HTTP metrics are managed by Vert.x, and are exposed by the Vert.x metrics SPI.
 * However, some of the metrics are not exposed by the SPI, and are only available through the Vert.x HTTP SPI.
 * <p>
 * Thus, we need to extend the Vert.x HTTP metrics SPI to expose these metrics.
 */
public interface ExtendedQuarkusVertxHttpMetrics {

    /**
     * A no-op connection tracker.
     */
    ConnectionTracker NOOP_CONNECTION_TRACKER = new ConnectionTracker() {
        @Override
        public void onConnectionRejected() {
        }

        @Override
        public void initialize(int maxConnections, AtomicInteger current) {
        }
    };

    /**
     * Gets a tracker to report the number of active HTTP connection, the number of rejected connections, etc.
     *
     * @return the tracker.
     */
    ConnectionTracker getHttpConnectionTracker();

    interface ConnectionTracker {

        void onConnectionRejected();

        void initialize(int maxConnections, AtomicInteger current);
    }
}
