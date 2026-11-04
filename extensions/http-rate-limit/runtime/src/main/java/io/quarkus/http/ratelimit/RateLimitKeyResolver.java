package io.quarkus.http.ratelimit;

import java.util.Optional;

import io.smallrye.mutiny.Uni;

/**
 * Computes the rate limit key for a request; every built-in {@code request-key} is one, and an application supplies
 * its own as a CDI bean annotated {@code @Identifier("<name>")}, selected with {@code request-key=custom:<name>}. Runs on the
 * event loop; must not block.
 * <p>
 * A user supplied resolver must answer within {@code quarkus.http.rate-limit.key-resolver-timeout} (250 ms by
 * default). On failure or timeout, {@code key-resolver-failure=reject} (the default) answers 503.
 * Explicitly set {@code ip} to use the client address instead. A warning is logged at most once per warning interval per rule.
 * Returning an empty key deliberately skips
 * the rule and does not trigger this failure handling.
 */
public interface RateLimitKeyResolver {

    /**
     * @param context the request being evaluated
     * @return the key, or empty if the rule does not apply to this request
     */
    Uni<Optional<String>> resolve(RateLimitContext context);
}
