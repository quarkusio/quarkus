package io.quarkus.security.spi.runtime;

import jakarta.inject.Inject;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.BlockingOperationNotAllowedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.smallrye.mutiny.Uni;

public abstract class AbstractSecurityIdentityAssociation implements CurrentIdentityAssociation {

    private volatile SecurityIdentity identity;
    private volatile Uni<SecurityIdentity> deferredIdentity;

    public AbstractSecurityIdentityAssociation() {
    }

    public AbstractSecurityIdentityAssociation(IdentityProviderManager identityProviderManager) {
        this.identityProviderManager = identityProviderManager;
    }

    @Inject
    IdentityProviderManager identityProviderManager;

    @Override
    public void setIdentity(SecurityIdentity identity) {
        this.identity = identity;
        this.deferredIdentity = null;
    }

    @Override
    public void setIdentity(Uni<SecurityIdentity> identity) {
        this.identity = null;
        this.deferredIdentity = identity;
    }

    public Uni<SecurityIdentity> getDeferredIdentity() {
        if (deferredIdentity != null) {
            return deferredIdentity;
        } else if (identity != null) {
            return Uni.createFrom().item(identity);
        } else {
            return deferredIdentity = identityProviderManager.authenticate(AnonymousAuthenticationRequest.INSTANCE);
        }
    }

    @Override
    public SecurityIdentity getIdentity() {
        if (identity == null) {
            if (deferredIdentity != null) {
                if (BlockingOperationControl.isBlockingAllowed()) {
                    identity = deferredIdentity.await().indefinitely();
                } else {
                    throw new BlockingOperationNotAllowedException(
                            "Cannot call getIdentity() from the IO thread when lazy authentication " +
                                    "is in use, as resolving the identity may block the thread. Instead you should inject the "
                                    +
                                    "CurrentIdentityAssociation, call CurrentIdentityAssociation#getDeferredIdentity() and " +
                                    "subscribe to the Uni.");
                }
            }
            if (identity == null) {
                identity = identityProviderManager.authenticate(AnonymousAuthenticationRequest.INSTANCE).await().indefinitely();
            }
        }
        return identity;
    }

}
