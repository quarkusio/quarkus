package io.quarkus.vertx.http.runtime.logstream;

import static org.jboss.logmanager.Level.ALL;
import static org.jboss.logmanager.Level.CONFIG;
import static org.jboss.logmanager.Level.DEBUG;
import static org.jboss.logmanager.Level.ERROR;
import static org.jboss.logmanager.Level.FATAL;
import static org.jboss.logmanager.Level.FINE;
import static org.jboss.logmanager.Level.FINER;
import static org.jboss.logmanager.Level.FINEST;
import static org.jboss.logmanager.Level.INFO;
import static org.jboss.logmanager.Level.OFF;
import static org.jboss.logmanager.Level.SEVERE;
import static org.jboss.logmanager.Level.TRACE;
import static org.jboss.logmanager.Level.WARN;
import static org.jboss.logmanager.Level.WARNING;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;

import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;

import io.quarkus.vertx.http.runtime.devmode.Json;

/**
 * Allow controlling to the log levels
 */
public class LogController {
    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(LogController.class);

    private LogController() {
    }

    public static Json.JsonArrayBuilder getLevels() {
        Json.JsonArrayBuilder array = Json.array();
        for (String level : LEVELS) {
            array.add(level);
        }
        return array;
    }

    public static Json.JsonArrayBuilder getLoggers() {
        LogContext logContext = LogContext.getLogContext();
        TreeMap<String, Json.JsonObjectBuilder> loggerMap = new TreeMap<>();

        Enumeration<String> loggerNames = logContext.getLoggerNames();
        while (loggerNames.hasMoreElements()) {
            String loggerName = loggerNames.nextElement();
            Json.JsonObjectBuilder jsonObject = getLogger(loggerName);
            if (jsonObject != null) {
                loggerMap.put(loggerName, jsonObject);
            }
        }

        List<Json.JsonObjectBuilder> orderedLoggers = new ArrayList<>(loggerMap.values());
        Json.JsonArrayBuilder jsonArray = Json.array();
        jsonArray.addAll(orderedLoggers);
        return jsonArray;
    }

    public static Json.JsonObjectBuilder getLogger(String loggerName) {
        LogContext logContext = LogContext.getLogContext();
        if (loggerName != null && !loggerName.isEmpty()) {
            Logger logger = logContext.getLogger(loggerName);
            Json.JsonObjectBuilder jsonObject = Json.object();
            jsonObject.put("name", loggerName);
            jsonObject.put("effectiveLevel", getEffectiveLogLevel(logger));
            jsonObject.put("configuredLevel", getConfiguredLogLevel(logger));
            return jsonObject;
        }
        return null;
    }

    public static void updateLogLevel(String loggerName, String levelValue) {
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
    }

    private static String getConfiguredLogLevel(Logger logger) {
        java.util.logging.Level level = logger.getLevel();
        return level != null ? level.getName() : null;
    }

    private static String getEffectiveLogLevel(Logger logger) {
        if (logger == null) {
            return null;
        }
        if (logger.getLevel() != null) {
            return logger.getLevel().getName();
        }
        return getEffectiveLogLevel(logger.getParent());
    }

    public static final List<String> LEVELS = List.of(
            OFF.getName(),
            SEVERE.getName(),
            ERROR.getName(),
            FATAL.getName(),
            WARNING.getName(),
            WARN.getName(),
            INFO.getName(),
            DEBUG.getName(),
            TRACE.getName(),
            CONFIG.getName(),
            FINE.getName(),
            FINER.getName(),
            FINEST.getName(),
            ALL.getName());
}
