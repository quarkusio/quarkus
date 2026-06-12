package io.quarkus.oidc.runtime;

import io.quarkus.oidc.common.ClientAttester;
import io.quarkus.oidc.common.runtime.AttestationKeyRegistry;
import io.quarkus.oidc.common.runtime.AttestationKeyRegistry.AttestationJwtContext;
import io.smallrye.mutiny.Uni;

public class DefaultClientAttester implements ClientAttester {

    private final AttestationKeyRegistry registry;
    private final String tenantId;

    public DefaultClientAttester(AttestationKeyRegistry registry, String tenantId) {
        this.registry = registry;
        this.tenantId = tenantId;
    }

    @Override
    public Uni<String> attest(ClientAttestationContext context) {
        AttestationJwtContext jwtContext = registry.getAttestationJwtContext(tenantId);
        if (jwtContext == null) {
            return Uni.createFrom().failure(
                    new RuntimeException("No attestation JWT context is registered for client: " + context.clientId()));
        }
        return Uni.createFrom().item(OidcUtils.buildClientAttestationJwt(context.clientId(), jwtContext));
    }
}
