package io.quarkus.websockets.next.runtime;

import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.setRoutingContextAttribute;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.WebSocketServerException;
import io.quarkus.websockets.next.runtime.spi.security.WebSocketIdentityUpdateRequest;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public final class SecuritySupport {

    public static final String QUARKUS_IDENTITY_EXPIRE_TIME = "quarkus.identity.expire-time";
    private static final Logger LOG = Logger.getLogger(SecuritySupport.class);
    static final SecuritySupport NOOP = new SecuritySupport(null, null, null, null);

    private final Instance<CurrentIdentityAssociation> currentIdentity;
    private final ManagedContext requestContext;
    private final RoutingContext routingContext;
    private volatile SecurityIdentity identity;
    private volatile Runnable onClose;

    SecuritySupport(Instance<CurrentIdentityAssociation> currentIdentity, SecurityIdentity identity,
            WebSocketConnectionImpl connection, RoutingContext routingContext) {
        this.currentIdentity = currentIdentity;
        if (this.currentIdentity != null) {
            this.identity = Objects.requireNonNull(identity);
            this.onClose = closeConnectionWhenIdentityExpired(routingContext.vertx(), connection, this.identity);
        } else {
            this.identity = null;
            this.onClose = null;
        }
        this.requestContext = Arc.container().requestContext();
        this.routingContext = routingContext;
    }

    /**
     * This method is called before an endpoint callback is invoked.
     */
    void start() {
        if (currentIdentity != null && requestContext.isActive()) {
            // If the request context is active then set the current identity
            CurrentIdentityAssociation current = currentIdentity.get();
            current.setIdentity(identity);
        }
    }

    void onClose() {
        if (onClose != null) {
            onClose.run();
        }
    }

    CompletionStage<SecurityIdentity> updateSecurityIdentity(String accessToken, WebSocketConnectionImpl connection,
            IdentityProviderManager identityProviderManager) {
        var authenticationRequest = new WebSocketIdentityUpdateRequest(new TokenCredential(accessToken, "bearer"),
                this.identity);
        return identityProviderManager
                .authenticate(setRoutingContextAttribute(authenticationRequest, routingContext))
                .onItem().ifNull().failWith(AuthenticationFailedException::new)
                .invoke(newIdentity -> this.updateSecurityIdentity(newIdentity, connection))
                .onFailure().invoke(throwable -> LOG.debug(
                        "Failed to update SecurityIdentity attached to the WebSocket connection with id " + connection.id(),
                        throwable))
                .convert().toCompletionStage();
    }

    private synchronized void updateSecurityIdentity(SecurityIdentity updatedIdentity, WebSocketConnectionImpl connection) {
        if (connection.isClosed()) {
            return;
        }
        if (updatedIdentity.isAnonymous()) {
            throw new AuthenticationFailedException("Updated SecurityIdentity is anonymous");
        }
        if (LOG.isDebugEnabled()) {
            Long expireAt = updatedIdentity.getAttribute(QUARKUS_IDENTITY_EXPIRE_TIME);
            String path = routingContext.normalizedPath();
            String principalName = updatedIdentity.getPrincipal().getName();
            LOG.debugf(
                    "Updated 'SecurityIdentity' with principal name '%s' used by WebSocket connection '%s' and path '%s', the new SecurityIdentity expires at '%d'",
                    principalName, connection.id(), path, expireAt);
        }
        String previousPrincipalName = this.identity.getPrincipal().getName();
        String currentPrincipalName = updatedIdentity.getPrincipal().getName();
        if (!previousPrincipalName.equals(currentPrincipalName)) {
            throw new WebSocketServerException(
                    "New SecurityIdentity principal name '%s' is different than previous principal name '%s'. SecurityIdentity update is aborted"
                            .formatted(currentPrincipalName, previousPrincipalName));
        }
        onClose(); // cancel previous timer that closes connection when identity expired
        this.identity = updatedIdentity;
        this.onClose = closeConnectionWhenIdentityExpired(routingContext.vertx(), connection, updatedIdentity);
        if (connection.isClosed()) {
            // it could be that while we were updating identity, connection has been closed
            // in that case, cancel timer we created few lines above (done this way to avoid race)
            onClose();
        } else {
            // SecurityIdentity CDI bean is proxy, so this should switch identity even in already injected beans
            start();
        }
    }

    private static Runnable closeConnectionWhenIdentityExpired(Vertx vertx, WebSocketConnectionImpl connection,
            SecurityIdentity identity) {
        if (identity.getAttribute(QUARKUS_IDENTITY_EXPIRE_TIME) instanceof Long expireAt) {
            long timerId = vertx.setTimer(TimeUnit.SECONDS.toMillis(expireAt) - System.currentTimeMillis(),
                    ignored -> connection
                            .close(new CloseReason(1008, "Authentication expired"))
                            .subscribe()
                            .with(
                                    v -> LOG.tracef("Closed connection due to expired authentication: %s", connection),
                                    e -> LOG.errorf("Unable to close connection [%s] after authentication "
                                            + "expired due to unhandled failure: %s", connection, e)));
            return () -> vertx.cancelTimer(timerId);
        }
        return null;
    }
}
