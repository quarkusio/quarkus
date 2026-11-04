package io.quarkus.http.ratelimit.runtime.internal.keys;

import java.util.Optional;

import jakarta.inject.Singleton;

import io.quarkus.http.ratelimit.RateLimitContext;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

@Singleton
@Identifier("global")
public class GlobalKeyResolver implements ImmediateKeyResolver {

    @Override
    public Uni<Optional<String>> resolve(RateLimitContext context) {
        return Uni.createFrom().item(Optional.of("global"));
    }
}
