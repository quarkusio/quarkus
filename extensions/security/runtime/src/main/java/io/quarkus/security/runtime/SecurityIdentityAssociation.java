package io.quarkus.security.runtime;

import java.security.Principal;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;

@RequestScoped
public class SecurityIdentityAssociation implements CurrentIdentityAssociation {

    private volatile SecurityIdentity identity;

    @Inject
    IdentityProviderManager identityProviderManager;

    @Produces
    @RequestScoped
    Principal principal() {
        //TODO: as this is request scoped we loose the type of the Principal
        //if this is important you can just inject the identity
        return new Principal() {
            @Override
            public String getName() {
                return getIdentity().getPrincipal().getName();
            }
        };
    }

    @Override
    public SecurityIdentity setIdentity(SecurityIdentity identity) {
        SecurityIdentity old = this.identity;
        this.identity = identity;
        return old;
    }

    @Override
    public SecurityIdentity getIdentity() {
        if (identity == null) {
            identity = identityProviderManager.authenticate(AnonymousAuthenticationRequest.INSTANCE)
                    .toCompletableFuture()
                    .join();
        }
        return identity;
    }
}
