package io.quarkus.http.ratelimit.runtime.internal.keys;

import java.util.Optional;

import jakarta.inject.Singleton;

import io.quarkus.http.ratelimit.RateLimitContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

/**
 * Keys a request by its non-anonymous principal; a request without one is keyed by its client address, exactly like the
 * {@code ip} key.
 */
@Singleton
@Identifier("principal")
public class PrincipalKeyResolver implements ImmediateKeyResolver {

    @Override
    public Uni<Optional<String>> resolve(RateLimitContext context) {
        Optional<SecurityIdentity> identity = context.identity();
        String key;
        if (identity.isPresent() && !identity.get().isAnonymous()) {
            key = "principal:" + identity.get().getPrincipal().getName();
        } else {
            key = IpKeyResolver.key(context.routingContext().request());
        }
        return Uni.createFrom().item(Optional.of(key));
    }
}
