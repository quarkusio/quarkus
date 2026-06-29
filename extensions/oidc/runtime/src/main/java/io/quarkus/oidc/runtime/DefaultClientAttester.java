package io.quarkus.oidc.runtime;

import jakarta.inject.Singleton;

import io.quarkus.arc.DefaultBean;
import io.quarkus.oidc.common.ClientAttester;
import io.quarkus.oidc.common.runtime.AttestationKeyRegistry.AttestationJwtContext;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.smallrye.mutiny.Uni;

@Singleton
@DefaultBean
public class DefaultClientAttester implements ClientAttester {

    private final io.quarkus.oidc.common.runtime.AttestationKeyRegistry registry;

    DefaultClientAttester(io.quarkus.oidc.common.runtime.AttestationKeyRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Uni<String> attest(ClientAttestationContext context) {
        AttestationJwtContext ctx = registry.getAttestationJwtContext(context.clientId());
        if (ctx == null) {
            return Uni.createFrom().failure(
                    new RuntimeException("No attestation JWT context is registered for client: " + context.clientId()));
        }
        return Uni.createFrom().item(OidcCommonUtils.buildClientAttestationJwt(context.clientId(), ctx));
    }
}
