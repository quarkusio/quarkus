package io.quarkus.vertx.http.runtime.devmode;

import java.util.Optional;

import io.quarkus.runtime.configuration.MemorySize;

record RemoteDevBodyLimits(long requestLimit, long aggregateLimit, int activeCollectorLimit) {

    static final long DEFAULT_REQUEST_LIMIT = 10L * 1024 * 1024;
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
