package io.quarkus.websockets.next.runtime;

import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.setRoutingContextAttribute;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.WebSocketServerException;
import io.quarkus.websockets.next.runtime.spi.security.WebSocketIdentityUpdateRequest;
import io.vertx.ext.web.RoutingContext;

public final class SecuritySupport {

    public static final String QUARKUS_IDENTITY_EXPIRE_TIME = "quarkus.identity.expire-time";
    private static final Logger LOG = Logger.getLogger(SecuritySupport.class);
    static final SecuritySupport NOOP = new SecuritySupport(null, null, null);

    private final RoutingContext routingContext;
    private volatile SecurityIdentity identity;
    private volatile Runnable onClose;

    SecuritySupport(SecurityIdentity identity, WebSocketConnectionImpl connection, RoutingContext routingContext) {
        this.identity = identity;
        this.onClose = closeConnectionWhenIdentityExpired(routingContext, connection, this.identity);
        this.routingContext = routingContext;
    }

    void onClose() {
        if (onClose != null) {
            onClose.run();
        }
    }

    SecurityIdentity getIdentity() {
        return identity;
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
        // this shouldn't be necessary (and probably isn't) but updating ctx it just to stay on the safe side
        this.routingContext.setUser(new QuarkusHttpUser(updatedIdentity));
        this.onClose = closeConnectionWhenIdentityExpired(routingContext, connection, updatedIdentity);
        if (connection.isClosed()) {
            // it could be that while we were updating identity, connection has been closed
            // in that case, cancel timer we created few lines above (done this way to avoid race)
            onClose();
        }
    }

    private static Runnable closeConnectionWhenIdentityExpired(RoutingContext routingContext,
            WebSocketConnectionImpl connection, SecurityIdentity identity) {
        if (identity != null && identity.getAttribute(QUARKUS_IDENTITY_EXPIRE_TIME) instanceof Long expireAt) {
            var vertx = routingContext.vertx();
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
