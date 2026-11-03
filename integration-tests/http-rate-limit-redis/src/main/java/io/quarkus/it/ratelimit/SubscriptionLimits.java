package io.quarkus.it.ratelimit;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.http.ratelimit.RateLimit;
import io.quarkus.http.ratelimit.RateLimitContext;
import io.quarkus.http.ratelimit.RateLimitResolver;
import io.quarkus.http.ratelimit.RateLimits;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

/**
 * Synthetic subscription data for the JVM/native tests. Real applications should use authenticated identity or
 * trusted subscription data, rather than trusting a client-provided tier.
 */
@Singleton
@Identifier("subscription")
public class SubscriptionLimits implements RateLimitResolver {
    @Inject
    RateLimits limits;

    @Override
    public Uni<RateLimit> resolve(RateLimitContext context) {
        String client = context.routingContext().request().getHeader("X-Client");
        if (client != null && client.startsWith("unavailable-")) {
            return Uni.createFrom().failure(new IllegalStateException("subscription unavailable"));
        }
        return Uni.createFrom().item(limits.get(client != null && client.startsWith("paid-") ? "paid" : "free"));
    }
}
