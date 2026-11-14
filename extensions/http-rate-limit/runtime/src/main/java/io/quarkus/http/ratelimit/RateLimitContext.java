package io.quarkus.http.ratelimit;

import java.util.Optional;

import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

/**
 * What a {@link RateLimitKeyResolver} or {@link RateLimitResolver} can see about the request being evaluated.
 */
public interface RateLimitContext {

    RoutingContext routingContext();

    /**
     * @return the identity in the post-authentication phase, an anonymous one when the request is not authenticated;
     *         empty in the pre-authentication phase
     */
    Optional<SecurityIdentity> identity();

    String rule();

    RateLimitPhase phase();
}
