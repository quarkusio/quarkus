package io.quarkus.runtime.logging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.logging.Logger;

public class LogCleanupFilter implements Filter {

    private Map<String, LogCleanupFilterElement> filterElements = new HashMap<>();

    public LogCleanupFilter(List<LogCleanupFilterElement> filterElements) {
        for (LogCleanupFilterElement element : filterElements) {
            this.filterElements.put(element.getLoggerName(), element);
        }
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        // Only allow filtering messages of warning level and lower
        if (record.getLevel().intValue() > Level.WARNING.intValue()) {
            return true;
        }
        LogCleanupFilterElement filterElement = filterElements.get(record.getLoggerName());
        if (filterElement != null) {
            for (String messageStart : filterElement.getMessageStarts()) {
                if (record.getMessage().startsWith(messageStart)) {
                    record.setLevel(filterElement.getTargetLevel());
                    if (filterElement.getTargetLevel().intValue() <= org.jboss.logmanager.Level.TRACE.intValue()) {
                        return Logger.getLogger(record.getLoggerName()).isTraceEnabled();
                    } else {
                        return Logger.getLogger(record.getLoggerName()).isDebugEnabled();
                    }
                }
            }
        }
        return true;
    }

}
