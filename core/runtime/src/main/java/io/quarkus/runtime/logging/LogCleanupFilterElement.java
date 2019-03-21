package io.quarkus.runtime.logging;

import java.util.List;

public class LogCleanupFilterElement {
    private String loggerName;
    private List<String> messageStarts;

    // Required by template
    public LogCleanupFilterElement() {
    }

    public LogCleanupFilterElement(String loggerName, List<String> messageStarts) {
        this.loggerName = loggerName;
        this.messageStarts = messageStarts;
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

    public void setMessageStart(List<String> messageStarts) {
        this.messageStarts = messageStarts;
    }
}