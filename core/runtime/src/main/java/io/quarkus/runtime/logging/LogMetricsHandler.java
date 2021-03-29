package io.quarkus.runtime.logging;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.jboss.logmanager.Level;

/**
 * Measures the number of log messages based on logger configurations quarkus.log.level and quarkus.log.category.*.level
 * <p>
 * It should reflect the values of the handler that logs the most, since best practice is to align its level with the root
 * level.
 * <p>
 * Non-standard levels are counted with the lower standard level.
 */
public class LogMetricsHandler extends Handler {

    final NavigableMap<Integer, LongAdder> logCounters;

    public LogMetricsHandler(NavigableMap<Integer, LongAdder> logCounters) {
        this.logCounters = logCounters;
    }

    @Override
    public void publish(LogRecord record) {
        if (isLoggable(record)) {
            Entry<Integer, LongAdder> counter = logCounters.floorEntry(record.getLevel().intValue());
            if (counter != null) {
                counter.getValue().increment();
            } else {
                // Default to TRACE for anything lower
                logCounters.get(Level.TRACE.intValue()).increment();
            }
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
