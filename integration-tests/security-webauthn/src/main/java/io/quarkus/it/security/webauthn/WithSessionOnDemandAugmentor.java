package io.quarkus.it.security.webauthn;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;

import io.quarkus.hibernate.reactive.panache.common.WithSessionOnDemand;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class WithSessionOnDemandAugmentor implements SecurityIdentityAugmentor {

    // required to reproduce https://github.com/quarkusio/quarkus/issues/47259
    @Override
    @ActivateRequestContext
    @WithSessionOnDemand
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        return Uni.createFrom().item(identity);
    }
}
