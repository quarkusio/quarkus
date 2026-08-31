package io.quarkus.grpc.runtime;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.jboss.logging.Logger;

import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpConnection;

/**
 * Enforces the gRPC {@code MAX_CONNECTION_AGE} and {@code MAX_CONNECTION_AGE_GRACE} semantics for
 * connections carrying gRPC traffic.
 * <p>
 * When a connection reaches the configured maximum age, a graceful shutdown is initiated: a
 * {@code GOAWAY} frame is sent so the client opens a new connection, in-flight calls are given the
 * configured grace period to complete, and then the connection is closed.
 */
final class MaxConnectionAgeEnforcer {

    private static final Logger LOGGER = Logger.getLogger(MaxConnectionAgeEnforcer.class.getName());

    /**
     * Grace period used when {@code max-connection-age-grace} is not set, mimicking the "infinite"
     * default of gRPC's {@code MAX_CONNECTION_AGE_GRACE}.
     */
    private static final Duration UNLIMITED_GRACE = Duration.ofDays(365);

    private final Vertx vertx;
    private final long maxAgeMillis;
    private final Duration grace;
    private final Set<HttpConnection> trackedConnections = ConcurrentHashMap.newKeySet();

    private MaxConnectionAgeEnforcer(Vertx vertx, Duration maxAge, Duration grace) {
        this.vertx = vertx;
        this.maxAgeMillis = Math.max(1, maxAge.toMillis());
        this.grace = grace;
    }

    /**
     * @return an enforcer for the given configuration, or {@code null} if {@code max-connection-age} is not set
     */
    static MaxConnectionAgeEnforcer of(Vertx vertx, GrpcServerConfiguration configuration) {
        if (configuration.maxConnectionAge().isEmpty()) {
            return null;
        }
        return new MaxConnectionAgeEnforcer(vertx, configuration.maxConnectionAge().get(),
                configuration.maxConnectionAgeGrace().orElse(UNLIMITED_GRACE));
    }

    /**
     * Starts tracking the given connection if it is not tracked yet, scheduling its graceful shutdown
     * once it reaches the maximum connection age.
     */
    void register(HttpConnection connection) {
        if (!trackedConnections.add(connection)) {
            return;
        }
        // Apply a +/-10% jitter, like grpc-java, to avoid all connections being shut down at the same time
        double jitterFactor = 0.9 + ThreadLocalRandom.current().nextDouble() * 0.2;
        long delay = Math.max(1, (long) (maxAgeMillis * jitterFactor));
        // The tracking entry is removed when the timer fires instead of using a close handler:
        // HttpConnection only holds a single close handler and other components (e.g. the HTTP connection
        // limiter) may have registered one already. Shutting down an already closed connection is a no-op.
        vertx.setTimer(delay, ignored -> {
            trackedConnections.remove(connection);
            LOGGER.debugf("gRPC connection reached its maximum age (%d ms), initiating graceful shutdown", maxAgeMillis);
            connection.shutdown(grace);
        });
    }
}
