package io.quarkus.websockets.next.runtime;

import java.util.Objects;

import org.jboss.logging.Logger;

import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.arc.ManagedContext;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;

/**
 * Per-endpoint CDI context support.
 */
public class ContextSupport {

    private static final Logger LOG = Logger.getLogger(ContextSupport.class);

    static final String WEB_SOCKET_CONN_KEY = WebSocketConnectionBase.class.getName();

    private final WebSocketConnectionBase connection;
    private final ContextState sessionContextState;
    private final ManagedContext sessionContext;
    private final ManagedContext requestContext;

    ContextSupport(WebSocketConnectionBase connection, ContextState sessionContextState,
            ManagedContext sessionContext,
            ManagedContext requestContext) {
        this.connection = connection;
        this.sessionContext = sessionContext;
        this.requestContext = requestContext;
        this.sessionContextState = sessionContext != null ? Objects.requireNonNull(sessionContextState) : null;
    }

    void start() {
        start(null);
    }

    void start(ContextState requestContextState) {
        LOG.debugf("Start contexts: %s", connection);
        startSession();
        if (requestContext != null) {
            requestContext.activate(requestContextState);
        }
    }

    void startSession() {
        if (sessionContext != null) {
            // Activate the captured session context
            sessionContext.activate(sessionContextState);
        }
    }

    void end(boolean terminateSession) {
        end(true, terminateSession);
    }

    void end(boolean terminateRequest, boolean terminateSession) {
        LOG.debugf("End contexts: %s [terminateRequest: %s, terminateSession: %s]", connection, terminateRequest,
                terminateSession);
        if (requestContext != null) {
            if (terminateRequest) {
                requestContext.terminate();
            } else {
                requestContext.deactivate();
            }
        }
        if (terminateSession) {
            // OnClose - terminate the session context
            endSession();
        } else if (sessionContext != null) {
            sessionContext.deactivate();
        }
    }

    void endSession() {
        if (sessionContext != null) {
            sessionContext.terminate();
        }
    }

    static Context createNewDuplicatedContext(Context context, WebSocketConnectionBase connection) {
        Context duplicated = VertxContext.createNewDuplicatedContext(context);
        VertxContextSafetyToggle.setContextSafe(duplicated, true);
        // We need to store the connection in the duplicated context
        // It's used to initialize the synthetic bean later on
        duplicated.putLocal(ContextSupport.WEB_SOCKET_CONN_KEY, connection);
        LOG.debugf("New vertx duplicated context [%s] created: %s", duplicated, connection);
        return duplicated;
    }

}
