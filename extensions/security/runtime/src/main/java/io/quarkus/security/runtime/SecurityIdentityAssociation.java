package io.quarkus.security.runtime;

import java.security.Principal;
import java.util.function.Consumer;

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
public class SecurityIdentityAssociation implements CurrentIdentityAssociation, Consumer<Uni<SecurityIdentity>> {

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
    public SecurityIdentity setIdentity(@Observes SecurityIdentity identity) {
        SecurityIdentity old = this.identity;
        this.identity = identity;
        return old;
    }

    public Uni<SecurityIdentity> getDeferredIdentity() {
        return deferredIdentity;
    }

    public void setDeferredIdentity(Uni<SecurityIdentity> deferredIdentity) {
        this.deferredIdentity = deferredIdentity;
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

    //THIS IS A TEMP HACK
    //a setDeferredIdentity and corresponding getter method needs to be added to the interface
    @Override
    public void accept(Uni<SecurityIdentity> securityIdentityUni) {
        deferredIdentity = securityIdentityUni;
    }
}
