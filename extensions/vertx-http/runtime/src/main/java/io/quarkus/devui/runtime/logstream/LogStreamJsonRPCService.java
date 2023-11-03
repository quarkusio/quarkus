package io.quarkus.devui.runtime.logstream;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;

import io.quarkus.arc.Arc;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Multi;
import io.vertx.core.json.JsonObject;

/**
 * This makes the log file available via json RPC
 */
public class LogStreamJsonRPCService {
    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(LogStreamJsonRPCService.class);

    @NonBlocking
    public String ping() {
        return "pong";
    }

    @NonBlocking
    public List<JsonObject> history() {
        LogStreamBroadcaster logStreamBroadcaster = Arc.container().instance(LogStreamBroadcaster.class).get();
        LinkedBlockingQueue<JsonObject> history = logStreamBroadcaster.getHistory();
        return new ArrayList<>(history);
    }

    public Multi<JsonObject> streamLog() {
        LogStreamBroadcaster logStreamBroadcaster = Arc.container().instance(LogStreamBroadcaster.class).get();
        return logStreamBroadcaster.getLogStream();
    }

    @NonBlocking
    public List<JsonObject> getLoggers() {
        LogContext logContext = LogContext.getLogContext();
        List<JsonObject> values = new ArrayList<>();
        Enumeration<String> loggerNames = logContext.getLoggerNames();
        while (loggerNames.hasMoreElements()) {
            String loggerName = loggerNames.nextElement();
            JsonObject jsonObject = getLogger(loggerName);
            if (jsonObject != null) {
                values.add(jsonObject);
            }
        }
        return values;
    }

    @NonBlocking
    public JsonObject getLogger(String loggerName) {
        LogContext logContext = LogContext.getLogContext();
        if (loggerName != null && !loggerName.isEmpty()) {
            Logger logger = logContext.getLogger(loggerName);
            return JsonObject.of(
                    "name", loggerName,
                    "effectiveLevel", getEffectiveLogLevel(logger),
                    "configuredLevel", getConfiguredLogLevel(logger));
        }
        return null;
    }

    @NonBlocking
    public JsonObject updateLogLevel(String loggerName, String levelValue) {
        LogContext logContext = LogContext.getLogContext();
        Logger logger = logContext.getLogger(loggerName);
        java.util.logging.Level level;
        if (levelValue == null || levelValue.isBlank()) {
            if (logger.getParent() != null) {
                level = logger.getParent().getLevel();
            } else {
                throw new IllegalArgumentException("The level of the root logger cannot be set to null");
            }
        } else {
            level = Level.parse(levelValue);
        }
        logger.setLevel(level);
        LOG.info("Log level updated [" + loggerName + "] changed to [" + levelValue + "]");

        return getLogger(loggerName);
    }

    private String getConfiguredLogLevel(Logger logger) {
        java.util.logging.Level level = logger.getLevel();
        return level != null ? level.getName() : null;
    }

    private String getEffectiveLogLevel(Logger logger) {
        if (logger == null) {
            return null;
        }
        if (logger.getLevel() != null) {
            return logger.getLevel().getName();
        }
        return getEffectiveLogLevel(logger.getParent());
    }
}
