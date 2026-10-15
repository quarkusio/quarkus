package io.quarkus.websockets.next.runtime;

import static io.quarkus.websockets.next.runtime.SecuritySupport.getSecurityIdentity;
import static io.quarkus.websockets.next.runtime.SecuritySupport.getSecurityIdentityUni;

import jakarta.enterprise.context.RequestScoped;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AbstractSecurityIdentityAssociation;
import io.smallrye.mutiny.Uni;

@RequestScoped
public class WebSocketSecurityIdentityAssociation extends AbstractSecurityIdentityAssociation {

    private final IdentityProviderManager identityProviderManager;
    private volatile boolean userChangedIdentity = false;

    WebSocketSecurityIdentityAssociation(IdentityProviderManager identityProviderManager) {
        this.identityProviderManager = identityProviderManager;
    }

    @Override
    public Uni<SecurityIdentity> getDeferredIdentity() {
        return getSecurityIdentityUni(userChangedIdentity, super::getDeferredIdentity);
    }

    @Override
    protected IdentityProviderManager getIdentityProviderManager() {
        return identityProviderManager;
    }

    @Override
    public void setIdentity(SecurityIdentity securityIdentity) {
        userChangedIdentity = true;
        super.setIdentity(securityIdentity);
    }

    @Override
    public void setIdentity(Uni<SecurityIdentity> uni) {
        userChangedIdentity = true;
        super.setIdentity(uni);
    }

    @Override
    public SecurityIdentity getIdentity() {
        return getSecurityIdentity(userChangedIdentity, super::getIdentity);
    }

    @Override
    public SecurityIdentity getIdentityValue() {
        return getSecurityIdentity(userChangedIdentity, super::getIdentityValue);
    }
}
