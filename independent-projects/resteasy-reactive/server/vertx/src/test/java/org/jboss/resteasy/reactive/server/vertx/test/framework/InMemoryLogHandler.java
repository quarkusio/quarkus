package org.jboss.resteasy.reactive.server.vertx.test.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.logmanager.ExtHandler;

public class InMemoryLogHandler extends ExtHandler {

    private final Predicate<LogRecord> predicate;

    public InMemoryLogHandler(Predicate<LogRecord> predicate) {
        this.predicate = predicate;
    }

    final List<LogRecord> records = new ArrayList<>();

    @Override
    public void publish(LogRecord record) {
        if (predicate.test(record)) {
            records.add(record);
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

    void clearRecords() {
        this.records.clear();
    }
}
