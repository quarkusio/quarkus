package io.quarkus.runtime.logging;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.logging.Logger;
import org.jboss.logmanager.ExtLogRecord;

public class LogCleanupFilter implements Filter {

    final Map<String, LogCleanupFilterElement> filterElements = new HashMap<>();
    public static final String SHUTDOWN_MESSAGE = " [Error Occurred After Shutdown]";

    private final LoggingSetupRecorder.ShutdownNotifier shutdownNotifier;

    public LogCleanupFilter(Collection<LogCleanupFilterElement> filterElements,
            LoggingSetupRecorder.ShutdownNotifier shutdownNotifier) {
        this.shutdownNotifier = shutdownNotifier;
        for (LogCleanupFilterElement element : filterElements) {
            this.filterElements.put(element.getLoggerName(), element);
        }
    }

    @Override
    public boolean isLoggable(LogRecord record) {

        //we also use this filter to add a warning about errors generated after shutdown
        if (record.getLevel().intValue() >= org.jboss.logmanager.Level.ERROR.intValue() && shutdownNotifier.shutdown) {
            if (!record.getMessage().endsWith(SHUTDOWN_MESSAGE)) {
                if (record instanceof ExtLogRecord) {
                    ExtLogRecord elr = (ExtLogRecord) record;
                    elr.setMessage(record.getMessage() + SHUTDOWN_MESSAGE, elr.getFormatStyle());
                } else {
                    record.setMessage(record.getMessage() + SHUTDOWN_MESSAGE);
                }
            }
        }
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
