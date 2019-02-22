package io.quarkus.runtime.logging;

public class LogCleanupFilterElement {
    private String loggerName;
    private String messageStart;

    // Required by template
    public LogCleanupFilterElement() {
    }

    public LogCleanupFilterElement(String loggerName, String messageStart) {
        this.loggerName = loggerName;
        this.messageStart = messageStart;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getMessageStart() {
        return messageStart;
    }

    public void setMessageStart(String messageStart) {
        this.messageStart = messageStart;
    }
}