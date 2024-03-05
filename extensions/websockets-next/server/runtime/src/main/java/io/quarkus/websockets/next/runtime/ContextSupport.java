package io.quarkus.websockets.next.runtime;

import org.jboss.logging.Logger;

import io.quarkus.arc.ManagedContext;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.runtime.WebSocketSessionContext.SessionContextState;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;

public class ContextSupport {

    private static final Logger LOG = Logger.getLogger(ContextSupport.class);

    private final WebSocketConnection connection;
    private final SessionContextState sessionContextState;
    private final WebSocketSessionContext sessionContext;
    private final ManagedContext requestContext;

    ContextSupport(WebSocketConnection connection, SessionContextState sessionContextState,
            WebSocketSessionContext sessionContext,
            ManagedContext requestContext) {
        this.connection = connection;
        this.sessionContextState = sessionContextState;
        this.sessionContext = sessionContext;
        this.requestContext = requestContext;
    }

    void start() {
        LOG.debugf("Start contexts: %s", connection);
        startSession();
        // Activate a new request context
        requestContext.activate();
    }

    void startSession() {
        // Activate the captured session context
        sessionContext.activate(sessionContextState);
    }

    void end(boolean terminateSession) {
        LOG.debugf("End contexts: %s", connection);
        requestContext.terminate();
        if (terminateSession) {
            // OnClose - terminate the session context
            endSession();
        } else {
            sessionContext.deactivate();
        }
    }

    void endSession() {
        sessionContext.terminate();
    }

    static Context createNewDuplicatedContext(Context context, WebSocketConnection connection) {
        Context duplicated = VertxContext.createNewDuplicatedContext(context);
        VertxContextSafetyToggle.setContextSafe(duplicated, true);
        // We need to store the connection in the duplicated context
        // It's used to initialize the synthetic bean later on
        duplicated.putLocal(WebSocketServerRecorder.WEB_SOCKET_CONN_KEY, connection);
        LOG.debugf("New vertx duplicated context [%s] created: %s", duplicated, connection);
        return duplicated;
    }

}
