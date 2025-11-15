package io.quarkus.devui.runtime.logstream;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.DevMCPEnableByDefault;
import io.quarkus.runtime.annotations.JsonRpcDescription;
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
    @JsonRpcDescription("Get a short Quarkus application log file history (last 60 lines)")
    public List<JsonObject> history() {
        LogStreamBroadcaster logStreamBroadcaster = Arc.container().instance(LogStreamBroadcaster.class).get();
        LinkedBlockingQueue<JsonObject> history = logStreamBroadcaster.getHistory();
        return new ArrayList<>(history);
    }

    @JsonRpcDescription("Stream the Quarkus application log file")
    public Multi<JsonObject> streamLog() {
        LogStreamBroadcaster logStreamBroadcaster = Arc.container().instance(LogStreamBroadcaster.class).get();
        return logStreamBroadcaster.getLogStream();
    }

    @NonBlocking
    @JsonRpcDescription("Get all the available loggers in this Quarkus application")
    @DevMCPEnableByDefault
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
    @JsonRpcDescription("Get a specific logger in this Quarkus application")
    @DevMCPEnableByDefault
    public JsonObject getLogger(@JsonRpcDescription("The name of the logger") String loggerName) {
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
    @JsonRpcDescription("Update a specific logger's log level in this Quarkus application")
    @DevMCPEnableByDefault
    public JsonObject updateLogLevel(@JsonRpcDescription("The name of the logger") String loggerName,
            @JsonRpcDescription("The new level of the logger") String levelValue) {
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
