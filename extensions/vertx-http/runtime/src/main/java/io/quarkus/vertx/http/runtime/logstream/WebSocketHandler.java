package io.quarkus.vertx.http.runtime.logstream;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;

import io.vertx.core.http.ServerWebSocket;

/**
 * Log handler for Logger Manager
 */
public class WebSocketHandler extends ExtHandler {

    private final ServerWebSocket session;

    public WebSocketHandler(ServerWebSocket session) {
        this.session = session;
        setFormatter(new JsonFormatter());
    }

    @Override
    public final void doPublish(final ExtLogRecord record) {
        // Don't log empty messages
        if (record.getMessage() == null || record.getMessage().isEmpty()) {
            return;
        }

        if (session != null && isLoggable(record)) {
            String message = getFormatter().format(record);
            try {
                session.writeTextMessage(message);
            } catch (Throwable ex) {
                session.close();
            }
        }

    }
}
