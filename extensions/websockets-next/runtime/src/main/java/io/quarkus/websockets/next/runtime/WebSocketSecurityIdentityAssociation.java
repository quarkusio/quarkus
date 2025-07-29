package io.quarkus.websockets.next.runtime;

import java.security.Principal;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AbstractSecurityIdentityAssociation;
import io.quarkus.vertx.http.runtime.security.VertxSecurityIdentityAssociation;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@RequestScoped
public class WebSocketSecurityIdentityAssociation implements CurrentIdentityAssociation {

    // delegate is used for non-WS Next connections, so that behavior is "as usual"
    private final AbstractSecurityIdentityAssociation delegate;

    WebSocketSecurityIdentityAssociation(IdentityProviderManager identityProviderManager,
            @ConfigProperty(name = "quarkus.http.auth.propagate-security-identity") boolean identityPropagationEnabled) {
        if (identityPropagationEnabled) {
            this.delegate = new VertxSecurityIdentityAssociation(identityProviderManager);
        } else {
            this.delegate = new AbstractSecurityIdentityAssociation(identityProviderManager) {
            };
        }
    }

    @Produces
    @RequestScoped
    public Principal principal() {
        return new Principal() {
            @Override
            public String getName() {
                return getIdentity().getPrincipal().getName();
            }
        };
    }

    @Override
    public Uni<SecurityIdentity> getDeferredIdentity() {
        SecurityIdentity securityIdentity = getSecurityIdentityFromCtx();
        if (securityIdentity != null) {
            return Uni.createFrom().item(securityIdentity);
        }
        return delegate.getDeferredIdentity();
    }

    @Override
    public void setIdentity(SecurityIdentity securityIdentity) {
        delegate.setIdentity(securityIdentity);
    }

    @Override
    public void setIdentity(Uni<SecurityIdentity> uni) {
        delegate.setIdentity(uni);
    }

    @Override
    public SecurityIdentity getIdentity() {
        SecurityIdentity securityIdentity = getSecurityIdentityFromCtx();
        if (securityIdentity != null) {
            return securityIdentity;
        }
        return delegate.getIdentity();
    }

    private static SecurityIdentity getSecurityIdentityFromCtx() {
        Context context = Vertx.currentContext();
        if (context != null && VertxContext.isDuplicatedContext(context)) {
            if (context.getLocal(ContextSupport.WEB_SOCKET_CONN_KEY) instanceof WebSocketConnectionImpl connection) {
                return connection.securitySupport().getIdentity();
            }
        }
        return null;
    }

}
