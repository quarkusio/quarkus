package io.quarkus.security.spi.runtime;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.BlockingOperationNotAllowedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.smallrye.mutiny.Uni;

/**
 * Base class for Quarkus built-in {@link CurrentIdentityAssociation} implementations to prevent code duplication.
 * Implementations must be {@link jakarta.enterprise.context.RequestScoped} to ensure thread safety.
 *
 * @see CurrentIdentityAssociation for more information
 */
public abstract class AbstractSecurityIdentityAssociation implements CurrentIdentityAssociation {

    private volatile SecurityIdentity identity;
    private volatile Uni<SecurityIdentity> deferredIdentity;

    /**
     * Returns the {@link IdentityProviderManager}.
     *
     * @return {@link IdentityProviderManager}
     */
    protected abstract IdentityProviderManager getIdentityProviderManager();

    /**
     * Sets the current {@link SecurityIdentity}, replacing any previous values set by this method
     * or {@link #setIdentity(Uni)}. This method should typically be called early when the CDI request
     * context is activated and should remain unchanged during the request.
     *
     * @param identity The new identity
     * @see CurrentIdentityAssociation#setIdentity(SecurityIdentity)
     */
    @Override
    public void setIdentity(SecurityIdentity identity) {
        this.identity = identity;
        this.deferredIdentity = null;
    }

    /**
     * Sets the current deferred {@link SecurityIdentity}, replacing any previous values set by this method
     * or {@link #setIdentity(SecurityIdentity)}. This method should typically be called early when the CDI request
     * context is activated and should remain unchanged during the request.
     *
     * @param identity The new identity
     * @see CurrentIdentityAssociation#setIdentity(Uni)
     */
    @Override
    public void setIdentity(Uni<SecurityIdentity> identity) {
        this.identity = null;
        this.deferredIdentity = identity;
    }

    /**
     * Retrieves a deferred {@link SecurityIdentity} that is resolved when the returned {@link Uni} is subscribed.
     * Subscribing may trigger authentication if the user is not already authenticated.
     * Quarkus Security memoizes this deferred identity, meaning authentication typically occurs only once
     * per CDI request context. Subsequent subscriptions are cheap.
     *
     * @return {@link SecurityIdentity}; never null
     * @see CurrentIdentityAssociation#getDeferredIdentity()
     */
    public Uni<SecurityIdentity> getDeferredIdentity() {
        if (deferredIdentity != null) {
            return deferredIdentity;
        } else if (identity != null) {
            return Uni.createFrom().item(identity);
        } else {
            return deferredIdentity = getIdentityProviderManager().authenticate(AnonymousAuthenticationRequest.INSTANCE);
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
                identity = getIdentityProviderManager().authenticate(AnonymousAuthenticationRequest.INSTANCE).await()
                        .indefinitely();
            }
        }
        return identity;
    }

}
