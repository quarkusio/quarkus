package io.quarkus.http.ratelimit;

import java.util.Optional;

import io.smallrye.mutiny.Uni;

/**
 * Computes the rate limit key for a request; every built-in {@code request-key} is one. Runs on the event loop; must not
 * block.
 */
public interface RateLimitKeyResolver {

    /**
     * @param context the request being evaluated
     * @return the key, or empty if the rule does not apply to this request
     */
    Uni<Optional<String>> resolve(RateLimitContext context);
}
