package io.quarkus.oidc.runtime;

import io.quarkus.oidc.common.ClientAttester;
import io.quarkus.oidc.runtime.AttestationKeyRegistry.AttestationJwtContext;
import io.smallrye.mutiny.Uni;

class DefaultClientAttester implements ClientAttester {

    private final AttestationJwtContext jwtContext;

    DefaultClientAttester(AttestationJwtContext jwtContext) {
        this.jwtContext = jwtContext;
    }

    @Override
    public Uni<String> attest(ClientAttestationContext context) {
        return Uni.createFrom().item(OidcUtils.buildClientAttestationJwt(context.clientId(), jwtContext));
    }

    @Override
    public boolean selfAttesting() {
        return true;
    }
}
