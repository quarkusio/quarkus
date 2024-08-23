package io.quarkus.websockets.next.runtime;

import org.jboss.logging.Logger;

import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.arc.ManagedContext;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.quarkus.websockets.next.runtime.WebSocketSessionContext.SessionContextState;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;

public class ContextSupport {

    private static final Logger LOG = Logger.getLogger(ContextSupport.class);

    static final String WEB_SOCKET_CONN_KEY = WebSocketConnectionBase.class.getName();

    private final WebSocketConnectionBase connection;
    private final SessionContextState sessionContextState;
    private final WebSocketSessionContext sessionContext;
    private final ManagedContext requestContext;

    ContextSupport(WebSocketConnectionBase connection, SessionContextState sessionContextState,
            WebSocketSessionContext sessionContext,
            ManagedContext requestContext) {
        this.connection = connection;
        this.sessionContextState = sessionContextState;
        this.sessionContext = sessionContext;
        this.requestContext = requestContext;
    }

    void start() {
        start(null);
    }

    void start(ContextState requestContextState) {
        LOG.debugf("Start contexts: %s", connection);
        startSession();
        requestContext.activate(requestContextState);
    }

    void startSession() {
        // Activate the captured session context
        sessionContext.activate(sessionContextState);
    }

    void end(boolean terminateSession) {
        end(true, terminateSession);
    }

    void end(boolean terminateRequest, boolean terminateSession) {
        LOG.debugf("End contexts: %s [terminateRequest: %s, terminateSession: %s]", connection, terminateRequest,
                terminateSession);
        if (terminateRequest) {
            requestContext.terminate();
        } else {
            requestContext.deactivate();
        }
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

    ContextState currentRequestContextState() {
        return requestContext.getStateIfActive();
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
