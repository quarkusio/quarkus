package io.quarkus.security.runtime;

import java.security.Principal;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.smallrye.mutiny.Uni;

@RequestScoped
public class SecurityIdentityAssociation implements CurrentIdentityAssociation {

    private volatile SecurityIdentity identity;
    private volatile Uni<SecurityIdentity> deferredIdentity;

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
    public void setIdentity(@Observes SecurityIdentity identity) {
        this.identity = identity;
        this.deferredIdentity = null;
    }

    @Override
    public void setIdentity(Uni<SecurityIdentity> identity) {
        this.identity = null;
        this.deferredIdentity = identity;
    }

    public Uni<SecurityIdentity> getDeferredIdentity() {
        return deferredIdentity;
    }

    @Override
    public SecurityIdentity getIdentity() {
        if (identity == null) {
            if (deferredIdentity != null) {
                identity = deferredIdentity.await().indefinitely();
            }
            if (identity == null) {
                identity = identityProviderManager.authenticate(AnonymousAuthenticationRequest.INSTANCE).await().indefinitely();
            }
        }
        return identity;
    }
}
