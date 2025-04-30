package io.quarkus.websockets.next.runtime;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.CloseReason;
import io.vertx.core.Vertx;

public class SecuritySupport {

    private static final Logger LOG = Logger.getLogger(SecuritySupport.class);
    static final SecuritySupport NOOP = new SecuritySupport(null, null, null, null);

    private final Instance<CurrentIdentityAssociation> currentIdentity;
    private final SecurityIdentity identity;
    private final Runnable onClose;
    private final ManagedContext requestContext;

    SecuritySupport(Instance<CurrentIdentityAssociation> currentIdentity, SecurityIdentity identity, Vertx vertx,
            WebSocketConnectionImpl connection) {
        this.currentIdentity = currentIdentity;
        if (this.currentIdentity != null) {
            this.identity = Objects.requireNonNull(identity);
            this.onClose = closeConnectionWhenIdentityExpired(vertx, connection, this.identity);
        } else {
            this.identity = null;
            this.onClose = null;
        }
        this.requestContext = Arc.container().requestContext();
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

    private static Runnable closeConnectionWhenIdentityExpired(Vertx vertx, WebSocketConnectionImpl connection,
            SecurityIdentity identity) {
        if (identity.getAttribute("quarkus.identity.expire-time") instanceof Long expireAt) {
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
