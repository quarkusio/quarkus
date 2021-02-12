package io.quarkus.vertx.http.runtime.logstream;

import static java.util.logging.Level.ALL;
import static java.util.logging.Level.CONFIG;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.OFF;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Allow controlling to the log levels
 */
public class LogController {

    public JsonArray getLevels() {
        return new JsonArray()
                .add(OFF.getName())
                .add(SEVERE.getName())
                .add(WARNING.getName())
                .add(INFO.getName())
                .add(CONFIG.getName())
                .add(FINE.getName())
                .add(FINER.getName())
                .add(FINEST.getName())
                .add(ALL.getName());
    }

    public JsonArray getLoggers() {
        TreeMap<String, JsonObject> loggerMap = new TreeMap<>();
        LogManager manager = LogManager.getLogManager();
        Enumeration<String> loggerNames = manager.getLoggerNames();
        while (loggerNames.hasMoreElements()) {
            String loggerName = loggerNames.nextElement();
            JsonObject jsonObject = getLogger(loggerName);
            if (jsonObject != null) {
                loggerMap.put(loggerName, jsonObject);
            }
        }

        List<JsonObject> orderedLoggers = new ArrayList<>(loggerMap.values());
        JsonArray jsonArray = new JsonArray(orderedLoggers);
        return jsonArray;
    }

    public JsonObject getLogger(String loggerName) {
        if (loggerName != null && !loggerName.isEmpty()) {
            Logger logger = Logger.getLogger(loggerName);
            JsonObject jsonObject = new JsonObject();
            jsonObject.put("name", loggerName);
            jsonObject.put("effectiveLevel", getEffectiveLogLevel(logger));
            jsonObject.put("configuredLevel", getConfiguredLogLevel(logger));
            return jsonObject;
        }
        return null;
    }

    public void updateLogLevel(String loggerName, String levelVal) {
        Logger logger = Logger.getLogger(loggerName);
        if (logger != null) {
            Level level = Level.parse(levelVal);
            logger.setLevel(level);
        }
    }

    private String getConfiguredLogLevel(Logger logger) {
        Level level = logger.getLevel();
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
