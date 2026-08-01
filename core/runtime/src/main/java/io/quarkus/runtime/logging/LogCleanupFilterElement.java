package io.quarkus.runtime.logging;

import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import org.jboss.logmanager.LogContext;

import io.quarkus.runtime.ObjectSubstitution;

public class LogCleanupFilterElement {
    private String loggerName;
    private List<String> messageStarts;
    private Level targetLevel;

    // Required by recorder
    public LogCleanupFilterElement() {

    }

    public LogCleanupFilterElement(String loggerName, List<String> messageStarts) {
        this.loggerName = loggerName;
        this.messageStarts = messageStarts;
    }

    public LogCleanupFilterElement(String loggerName, Level targetLevel, List<String> messageStarts) {
        this.loggerName = loggerName;
        this.messageStarts = messageStarts;
        this.targetLevel = targetLevel;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public List<String> getMessageStarts() {
        return messageStarts;
    }

    public void setMessageStarts(List<String> messageStarts) {
        this.messageStarts = messageStarts;
    }

    public Level getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(Level targetLevel) {
        this.targetLevel = targetLevel;
    }

    public static class LevelSubstitution implements ObjectSubstitution<Level, String> {
        @Override
        public String serialize(Level obj) {
            return obj.getName();
        }

        @Override
        public Level deserialize(String obj) {
            return LogContext.getLogContext().getLevelForName(obj.toUpperCase(Locale.ROOT));
        }
    }
}
