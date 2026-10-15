package io.quarkus.websockets.next.runtime;

import static io.quarkus.websockets.next.runtime.SecuritySupport.getSecurityIdentity;
import static io.quarkus.websockets.next.runtime.SecuritySupport.getSecurityIdentityUni;

import jakarta.enterprise.context.RequestScoped;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.DuplicatedContextSecurityIdentityAssociation;
import io.smallrye.mutiny.Uni;

@RequestScoped
public class WebSocketDuplicatedContextSecurityIdentityAssociation extends DuplicatedContextSecurityIdentityAssociation {

    private volatile boolean userChangedIdentity = false;

    private WebSocketDuplicatedContextSecurityIdentityAssociation(IdentityProviderManager identityProviderManager) {
        setIdentityProviderManager(identityProviderManager);
    }

    @Override
    public Uni<SecurityIdentity> getDeferredIdentity() {
        return getSecurityIdentityUni(userChangedIdentity, super::getDeferredIdentity);
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
