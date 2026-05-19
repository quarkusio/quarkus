package io.quarkus.devui.runtime.logstream;

import java.util.List;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.logging.DecorateStackUtil;
import io.vertx.core.json.JsonObject;

/**
 * Log handler for Logger Manager
 */
public class MutinyLogHandler extends ExtHandler {

    private LogStreamBroadcaster logStreamBroadcaster;
    private final boolean decorateStack;
    private final String srcMainJava;
    private final List<String> knownClasses;

    public MutinyLogHandler(boolean decorateStack, String srcMainJava, List<String> knownClasses) {
        this.decorateStack = decorateStack;
        this.srcMainJava = srcMainJava;
        this.knownClasses = knownClasses;
        setFormatter(new JsonFormatter());
    }

    @Override
    public final void doPublish(final ExtLogRecord record) {
        // Don't log empty messages
        if (record.getMessage() == null || record.getMessage().isEmpty()) {
            return;
        }

        // Filter out framework-internal loggers to prevent feedback loops.
        // These framework components (Netty WebSocket encoding, Vert.x event loop,
        // and our own log broadcasting code) emit logs during the act of broadcasting,
        // which would create infinite recursion. Filter all levels - the log stream
        // should never broadcast its own transport-layer logs regardless of level.
        String loggerName = record.getLoggerName();
        if (loggerName != null && (loggerName.startsWith("io.netty.") ||
                loggerName.startsWith("io.vertx.") ||
                loggerName.startsWith("io.quarkus.devui.runtime.logstream."))) {
            return;
        }

        if (isLoggable(record)) {
            LogStreamBroadcaster broadcaster = getBroadcaster();
            if (broadcaster != null) {
                JsonObject message = ((JsonFormatter) getFormatter()).toJsonObject(record);
                if (decorateStack) {
                    String decoratedString = DecorateStackUtil.getDecoratedString(record.getThrown(), this.srcMainJava,
                            knownClasses);
                    if (decoratedString != null) {
                        message.put("decoration", decoratedString);
                    }
                }

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
