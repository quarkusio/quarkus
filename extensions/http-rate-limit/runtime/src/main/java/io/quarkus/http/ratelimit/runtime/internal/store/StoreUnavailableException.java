package io.quarkus.http.ratelimit.runtime.internal.store;

import java.time.Duration;

/**
 * Internal refusal after the store failure policy has been applied. Never returned by a user store.
 */
public final class StoreUnavailableException extends RuntimeException {
    private final Duration retryAfter;

    StoreUnavailableException(Duration retryAfter) {
        super("Rate limit store unavailable", null, false, false);
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
