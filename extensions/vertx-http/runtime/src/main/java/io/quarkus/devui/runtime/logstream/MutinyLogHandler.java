package io.quarkus.devui.runtime.logstream;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;

import io.quarkus.arc.Arc;
import io.vertx.core.json.JsonObject;

/**
 * Log handler for Logger Manager
 */
public class MutinyLogHandler extends ExtHandler {

    private LogStreamBroadcaster logStreamBroadcaster;

    public MutinyLogHandler() {
        setFormatter(new JsonFormatter());
    }

    @Override
    public final void doPublish(final ExtLogRecord record) {
        // Don't log empty messages
        if (record.getMessage() == null || record.getMessage().isEmpty()) {
            return;
        }

        if (isLoggable(record)) {
            LogStreamBroadcaster broadcaster = getBroadcaster();
            if (broadcaster != null) {
                JsonObject message = ((JsonFormatter) getFormatter()).toJsonObject(record);
                broadcaster.onNext(message);
            }
        }
    }

    private LogStreamBroadcaster getBroadcaster() {
        synchronized (this) {
            if (this.logStreamBroadcaster == null && Arc.container() != null) {
                this.logStreamBroadcaster = Arc.container().instance(LogStreamBroadcaster.class).get();
            }
        }
        return this.logStreamBroadcaster;
    }
}
