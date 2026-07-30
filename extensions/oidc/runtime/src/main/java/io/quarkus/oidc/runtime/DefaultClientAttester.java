package io.quarkus.oidc.runtime;

import io.quarkus.oidc.common.ClientAttester;
import io.quarkus.oidc.common.runtime.AttestationKeyRegistry;
import io.quarkus.oidc.common.runtime.AttestationKeyRegistry.AttestationJwtContext;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.smallrye.mutiny.Uni;

public class DefaultClientAttester implements ClientAttester {

    private final AttestationKeyRegistry registry;

    public DefaultClientAttester(AttestationKeyRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Uni<String> attest(ClientAttestationContext context) {
        AttestationJwtContext jwtContext = registry.getAttestationJwtContext(context.clientId());
        if (jwtContext == null) {
            return Uni.createFrom().failure(
                    new RuntimeException("No attestation JWT context is registered for client: " + context.clientId()));
        }
        return Uni.createFrom().item(OidcCommonUtils.buildClientAttestationJwt(context.clientId(), jwtContext));
    }
}
