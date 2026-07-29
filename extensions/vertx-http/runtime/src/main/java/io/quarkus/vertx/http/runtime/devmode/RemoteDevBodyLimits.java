package io.quarkus.vertx.http.runtime.devmode;

import java.util.Optional;

import io.quarkus.runtime.configuration.MemorySize;

/**
 * Bounds remote-dev body collection independently of the normal router body handler.
 * <p>
 * The request limit follows {@code quarkus.http.limits.max-body-size}. When that general limit is disabled, remote
 * development retains the same 10 MiB default rather than accepting unauthenticated bodies without a bound. Aggregate
 * capacity is twice the per-request limit so a maximum-sized request can be processed while another is collected. The
 * collector limit still bounds concurrent spool files and worker writes for smaller requests.
 */
record RemoteDevBodyLimits(long requestLimit, long aggregateLimit, int activeCollectorLimit) {

    /** Matches the {@code quarkus.http.limits.max-body-size} default when that limit is disabled. */
    static final long DEFAULT_REQUEST_LIMIT = 10L * 1024 * 1024;

    /** Limits concurrent collectors to keep smaller requests concurrent without unbounded spool files or worker writes. */
    static final int DEFAULT_ACTIVE_COLLECTOR_LIMIT = 4;

    RemoteDevBodyLimits {
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("Remote-dev request body limit must be greater than zero");
        }
        if (aggregateLimit < requestLimit) {
            throw new IllegalArgumentException("Remote-dev aggregate body limit must not be smaller than the request limit");
        }
        if (activeCollectorLimit <= 0) {
            throw new IllegalArgumentException("Remote-dev active body collector limit must be greater than zero");
        }
    }

    /**
     * Derives remote-dev limits from the configured HTTP body limit or the finite fallback when it is disabled.
     */
    static RemoteDevBodyLimits from(Optional<MemorySize> configuredLimit) {
        long requestLimit;
        try {
            requestLimit = configuredLimit.map(MemorySize::asLongValue).orElse(DEFAULT_REQUEST_LIMIT);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Remote-dev request body limit exceeds the supported range", e);
        }
        long aggregateLimit = requestLimit > Long.MAX_VALUE / 2 ? Long.MAX_VALUE : requestLimit * 2;
        return new RemoteDevBodyLimits(requestLimit, aggregateLimit, DEFAULT_ACTIVE_COLLECTOR_LIMIT);
    }
}
