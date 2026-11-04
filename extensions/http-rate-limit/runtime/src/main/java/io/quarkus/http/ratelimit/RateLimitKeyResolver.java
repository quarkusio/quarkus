package io.quarkus.http.ratelimit;

import java.util.Optional;

import io.smallrye.mutiny.Uni;

/**
 * Computes the rate limit key for a request; every built-in {@code request-key} is one, and an application supplies
 * its own as a CDI bean annotated {@code @Identifier("<name>")}, selected with {@code request-key=custom:<name>}. Runs on the
 * event loop; must not block.
 * <p>
 * A user supplied resolver must answer within 250 ms. If it fails or does not answer in time, the request is keyed by
 * its client address, and a warning is logged at most once per warning interval per rule.
 */
public interface RateLimitKeyResolver {

    /**
     * @param context the request being evaluated
     * @return the key, or empty if the rule does not apply to this request
     */
    Uni<Optional<String>> resolve(RateLimitContext context);
}
