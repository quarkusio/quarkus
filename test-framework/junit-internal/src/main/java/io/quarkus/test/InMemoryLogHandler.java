package io.quarkus.test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.logmanager.ExtHandler;

import io.smallrye.common.constraint.Assert;

public class InMemoryLogHandler extends ExtHandler {

    private final Predicate<LogRecord> predicate;

    public InMemoryLogHandler(Predicate<LogRecord> predicate) {
        this.predicate = Assert.checkNotNullParam("predicate", predicate);
    }

    final List<LogRecord> records = new CopyOnWriteArrayList<>();

    @Override
    public void publish(LogRecord record) {
        if (isLoggable(record) && predicate.test(record)) {
            records.add(record);
        }
    }

    /**
     * Retroactively filters already-captured records when a new filter is installed,
     * so that records captured before the filter was set are also filtered.
     */
    @Override
    public void setFilter(Filter newFilter) throws SecurityException {
        super.setFilter(newFilter);
        if (newFilter != null) {
            records.removeIf(record -> !newFilter.isLoggable(record));
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public Level getLevel() {
        return Level.FINE;
    }

    @Override
    public void close() throws SecurityException {
        this.records.clear();
    }

    public List<LogRecord> getRecords() {
        return records;
    }

    void clearRecords() {
        this.records.clear();
    }
}
