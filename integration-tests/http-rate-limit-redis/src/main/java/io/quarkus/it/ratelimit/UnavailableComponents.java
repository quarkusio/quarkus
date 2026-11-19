package io.quarkus.it.ratelimit;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.http.ratelimit.RateLimitKeyResolver;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

@Singleton
public class UnavailableComponents {
    @Produces
    @Identifier("unavailable")
    RateLimitKeyResolver key() {
        return context -> Uni.createFrom().failure(new IllegalStateException("tenant unavailable"));
    }

    @Produces
    @Identifier("unavailable")
    RateLimitStore store() {
        return (key, cost, policy) -> Uni.createFrom().failure(new IllegalStateException("store unavailable"));
    }
}
