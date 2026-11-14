package io.quarkus.http.ratelimit.runtime.internal.rules;

import java.util.Optional;

import io.quarkus.http.ratelimit.RateLimitContext;
import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

record RateLimitContextImpl(RoutingContext routingContext, Optional<SecurityIdentity> identity, String rule,
        RateLimitPhase phase) implements RateLimitContext {
}
