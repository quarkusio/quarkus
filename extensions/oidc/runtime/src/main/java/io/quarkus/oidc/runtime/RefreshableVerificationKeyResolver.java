package io.quarkus.oidc.runtime;

import org.jose4j.keys.resolvers.VerificationKeyResolver;

import io.smallrye.mutiny.Uni;

public interface RefreshableVerificationKeyResolver extends VerificationKeyResolver {
    default Uni<Void> refresh() {
        return Uni.createFrom().voidItem();
    }
}