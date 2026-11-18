package io.quarkus.oidc.runtime;

import io.smallrye.jwt.auth.VerificationKeyResolver;
import io.smallrye.mutiny.Uni;

public interface RefreshableVerificationKeyResolver extends VerificationKeyResolver {
    default Uni<Void> refresh() {
        return Uni.createFrom().voidItem();
    }
}
