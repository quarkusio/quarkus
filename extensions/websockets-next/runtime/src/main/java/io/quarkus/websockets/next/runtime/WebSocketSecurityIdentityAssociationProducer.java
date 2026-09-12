package io.quarkus.websockets.next.runtime;

import java.util.function.Supplier;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;

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

public class WebSocketSecurityIdentityAssociationProducer {

    @Produces
    @RequestScoped
    CurrentIdentityAssociation produceCurrentIdentityAssociation(IdentityProviderManager identityProviderManager,
            @ConfigProperty(name = "quarkus.http.auth.propagate-security-identity") boolean identityPropagationEnabled) {
        if (identityPropagationEnabled) {
            return new WebSocketDuplicatedContextSecurityIdentityAssociation(identityProviderManager);
        } else {
            return new WebSocketSecurityIdentityAssociation(identityProviderManager);
        }
    }

    private static Uni<SecurityIdentity> getSecurityIdentityUni(boolean userChangedIdentity,
            Supplier<Uni<SecurityIdentity>> deferredIdentitySupplier) {
        if (userChangedIdentity) {
            return deferredIdentitySupplier.get();
        }

        SecuritySupport securitySupport = getSecuritySupportFromCtx();
        if (securitySupport != null) {
            if (securitySupport.getIdentity() != null) {
                return Uni.createFrom().item(securitySupport.getIdentity());
            }
            Uni<SecurityIdentity> deferredIdentity = securitySupport.getDeferredIdentity();
            if (deferredIdentity != null) {
                // calling to the delegate should return anonymous identity, so that we avoid NPEs
                return deferredIdentity.onItem().ifNull().switchTo(deferredIdentitySupplier::get);
            }
        }

        return deferredIdentitySupplier.get();
    }

    private static SecurityIdentity getSecurityIdentity(boolean userChangedIdentity,
            Supplier<SecurityIdentity> identitySupplier) {
        if (userChangedIdentity) {
            return identitySupplier.get();
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

        return identitySupplier.get();
    }

    private static SecuritySupport getSecuritySupportFromCtx() {
        Context context = Vertx.currentContext();
        if (context != null && VertxContext.isDuplicatedContext(context)) {
            if (ContextSupport.WebSocketContextLocalsProvider.WEB_SOCKET_CONN_LOCAL
                    .get(context) instanceof WebSocketConnectionImpl connection) {
                return connection.securitySupport();
            }
        }
        return null;
    }

    private static final class WebSocketDuplicatedContextSecurityIdentityAssociation
            extends DuplicatedContextSecurityIdentityAssociation {

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
        public SecurityIdentity getIdentityOrNull() {
            return getSecurityIdentity(userChangedIdentity, super::getIdentityOrNull);
        }
    }

    private static final class WebSocketSecurityIdentityAssociation extends AbstractSecurityIdentityAssociation {

        private volatile boolean userChangedIdentity = false;

        private final IdentityProviderManager identityProviderManager;

        private WebSocketSecurityIdentityAssociation(IdentityProviderManager identityProviderManager) {
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
        public SecurityIdentity getIdentityOrNull() {
            return getSecurityIdentity(userChangedIdentity, super::getIdentityOrNull);
        }
    }
}
