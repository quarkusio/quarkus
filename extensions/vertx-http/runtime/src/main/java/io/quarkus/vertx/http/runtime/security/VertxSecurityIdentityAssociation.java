package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.vertx.http.runtime.security.QuarkusHttpUser.DEFERRED_IDENTITY_KEY;

import java.security.Principal;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AbstractSecurityIdentityAssociation;
import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@RequestScoped
public class VertxSecurityIdentityAssociation extends AbstractSecurityIdentityAssociation {

    @Inject
    public VertxSecurityIdentityAssociation() {
    }

    public VertxSecurityIdentityAssociation(IdentityProviderManager identityProviderManager) {
        super(identityProviderManager);
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
        RoutingContext routingContext = getRoutingContext();
        if (routingContext != null) {
            SecurityIdentity securityIdentity = getSecurityIdentityFromCtx(routingContext);
            if (securityIdentity != null) {
                return Uni.createFrom().item(securityIdentity);
            }
            Uni<SecurityIdentity> identityUni = routingContext.get(DEFERRED_IDENTITY_KEY);
            if (identityUni != null) {
                return identityUni;
            }
        }
        return super.getDeferredIdentity();
    }

    @Override
    public SecurityIdentity getIdentity() {
        RoutingContext routingContext = getRoutingContext();
        if (routingContext != null) {
            SecurityIdentity securityIdentity = getSecurityIdentityFromCtx(routingContext);
            if (securityIdentity != null) {
                return securityIdentity;
            }
            securityIdentity = QuarkusHttpUser.getSecurityIdentityBlocking(routingContext, null);
            if (securityIdentity != null) {
                return securityIdentity;
            }
        }
        return super.getIdentity();
    }

    private static SecurityIdentity getSecurityIdentityFromCtx(RoutingContext routingContext) {
        if (routingContext.user() instanceof QuarkusHttpUser quarkusHttpUser) {
            return quarkusHttpUser.getSecurityIdentity();
        }
        return null;
    }

    private static RoutingContext getRoutingContext() {
        return ContextLocals.get(HttpSecurityUtils.ROUTING_CONTEXT_ATTRIBUTE, null);
    }
}
