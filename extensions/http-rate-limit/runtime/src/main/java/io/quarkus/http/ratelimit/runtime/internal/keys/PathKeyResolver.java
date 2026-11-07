package io.quarkus.http.ratelimit.runtime.internal.keys;

import java.util.Optional;

import jakarta.inject.Singleton;

import io.quarkus.http.ratelimit.RateLimitContext;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

@Singleton
@Identifier("path")
public class PathKeyResolver implements ImmediateKeyResolver {

    @Override
    public Uni<Optional<String>> resolve(RateLimitContext context) {
        // the canonical path the rules are matched on: /a;x=1 must share the bucket of /a
        return Uni.createFrom().item(
                Optional.of("path:" + HttpSecurityUtils.normalizePath(context.routingContext().normalizedPath())));
    }
}
