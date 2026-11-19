package io.quarkus.http.ratelimit;

import io.smallrye.mutiny.Uni;

/**
 * Selects a limit for the current request, for example from the user's subscription or roles.
 * Implement as a CDI bean annotated {@code @Identifier("<name>")} and select it with
 * {@code quarkus.http.rate-limit.rules."rule".limit-resolver=<name>}.
 * <p>
 * Return a configured limit from {@link RateLimits#get(String)}, or construct a {@link RateLimit}.
 * The request key still determines who shares the allowance: selecting the same named limit does not share counters.
 * Use {@link RateLimitContext#identity()} for user information; storage keys are not business identifiers.
 * <p>
 * Called on the event loop after key resolution; must not block. The timeout is
 * {@code quarkus.http.rate-limit.limit-resolver-timeout}. A failure, timeout or null result uses the rule's explicitly
 * configured {@code fallback-limit}, or answers 503 if none is configured. Return a limit for anonymous requests too.
 */
public interface RateLimitResolver {

    /**
     * @return the limit for this request, never null
     */
    Uni<RateLimit> resolve(RateLimitContext context);
}
