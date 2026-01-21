package io.quarkus.websockets.next.runtime;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AbstractSecurityIdentityAssociation;
import io.quarkus.vertx.http.runtime.security.DuplicatedContextSecurityIdentityAssociation;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@RequestScoped
public class WebSocketSecurityIdentityAssociation implements CurrentIdentityAssociation {

    // delegate is used for non-WS Next connections, so that behavior is "as usual"
    private final AbstractSecurityIdentityAssociation delegate;
    /**
     * This is a CDI bean and {@link SecurityIdentity} setter methods are public, therefore it is theoretically
     * possible users could change identity at any time, and we need to remember it.
     */
    private volatile boolean userChangedIdentity = false;

    @Inject
    WebSocketSecurityIdentityAssociation(IdentityProviderManager identityProviderManager,
            @ConfigProperty(name = "quarkus.http.auth.propagate-security-identity") boolean identityPropagationEnabled) {
        if (identityPropagationEnabled) {
            this.delegate = new DuplicatedContextSecurityIdentityAssociation()
                    .setIdentityProviderManager(identityProviderManager);
        } else {
            this.delegate = new AbstractSecurityIdentityAssociation() {
                @Override
                protected IdentityProviderManager getIdentityProviderManager() {
                    return identityProviderManager;
                }
            };
        }
    }

    @Override
    public Uni<SecurityIdentity> getDeferredIdentity() {
        if (userChangedIdentity) {
            return delegate.getDeferredIdentity();
        }

        SecuritySupport securitySupport = getSecuritySupportFromCtx();
        if (securitySupport != null) {
            if (securitySupport.getIdentity() != null) {
                return Uni.createFrom().item(securitySupport.getIdentity());
            }
            Uni<SecurityIdentity> deferredIdentity = securitySupport.getDeferredIdentity();
            if (deferredIdentity != null) {
                // calling to the delegate should return anonymous identity, so that we avoid NPEs
                return deferredIdentity.onItem().ifNull().switchTo(delegate::getDeferredIdentity);
            }
        }

        return delegate.getDeferredIdentity();
    }

    @Override
    public void setIdentity(SecurityIdentity securityIdentity) {
        userChangedIdentity = true;
        delegate.setIdentity(securityIdentity);
    }

    @Override
    public void setIdentity(Uni<SecurityIdentity> uni) {
        userChangedIdentity = true;
        delegate.setIdentity(uni);
    }

    @Override
    public SecurityIdentity getIdentity() {
        if (userChangedIdentity) {
            return delegate.getIdentity();
        }

        SecuritySupport securitySupport = getSecuritySupportFromCtx();
        if (securitySupport != null) {
            if (securitySupport.getIdentity() != null) {
                return securitySupport.getIdentity();
            }
            if (BlockingOperationControl.isBlockingAllowed()) {
                Uni<SecurityIdentity> deferredIdentity = securitySupport.getDeferredIdentity();
                if (deferredIdentity != null) {
                    SecurityIdentity resolvedIdentity = deferredIdentity.await().indefinitely();
                    if (resolvedIdentity != null) {
                        return resolvedIdentity;
                    }
                }
            }
        }

        return delegate.getIdentity();
    }

    private static SecuritySupport getSecuritySupportFromCtx() {
        Context context = Vertx.currentContext();
        if (context != null && VertxContext.isDuplicatedContext(context)) {
            if (context.getLocal(ContextSupport.WEB_SOCKET_CONN_KEY) instanceof WebSocketConnectionImpl connection) {
                return connection.securitySupport();
            }
        }
        return null;
    }

}
