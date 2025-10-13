package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.vertx.http.runtime.security.QuarkusHttpUser.DEFERRED_IDENTITY_KEY;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import io.quarkus.arc.DefaultBean;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AbstractSecurityIdentityAssociation;
import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@DefaultBean
@RequestScoped
public class DuplicatedContextSecurityIdentityAssociation extends AbstractSecurityIdentityAssociation {

    private IdentityProviderManager identityProviderManager;

    @Inject
    public DuplicatedContextSecurityIdentityAssociation setIdentityProviderManager(
            IdentityProviderManager identityProviderManager) {
        this.identityProviderManager = identityProviderManager;
        return this;
    }

    @Override
    protected IdentityProviderManager getIdentityProviderManager() {
        return identityProviderManager;
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
