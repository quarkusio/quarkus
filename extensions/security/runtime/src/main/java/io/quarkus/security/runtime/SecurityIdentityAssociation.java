package io.quarkus.security.runtime;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import io.quarkus.arc.DefaultBean;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.spi.runtime.AbstractSecurityIdentityAssociation;

@DefaultBean
@RequestScoped
public class SecurityIdentityAssociation extends AbstractSecurityIdentityAssociation {

    @Inject
    IdentityProviderManager identityProviderManager;

    @Override
    protected IdentityProviderManager getIdentityProviderManager() {
        return identityProviderManager;
    }
}
