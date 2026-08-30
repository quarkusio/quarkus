package io.quarkus.oidc.runtime;

import io.quarkus.oidc.common.ClientAttester;
import io.quarkus.oidc.runtime.AttestationKeyRegistry.AttestationJwtContext;
import io.smallrye.mutiny.Uni;

public class DefaultClientAttester implements ClientAttester {

    private final AttestationKeyRegistry registry;

    public DefaultClientAttester(AttestationKeyRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Uni<String> attest(ClientAttestationContext context) {
        String tenantId = (String) context.extraParams().get(OidcUtils.TENANT_ID_ATTRIBUTE);
        AttestationJwtContext jwtContext = registry.getAttestationJwtContext(tenantId);
        if (jwtContext == null) {
            return Uni.createFrom().failure(
                    new RuntimeException("No attestation JWT context is registered for client: " + context.clientId()));
        }
        return Uni.createFrom().item(OidcUtils.buildClientAttestationJwt(context.clientId(), jwtContext));
    }

    @Override
    public boolean selfAttesting() {
        return true;
    }
}
