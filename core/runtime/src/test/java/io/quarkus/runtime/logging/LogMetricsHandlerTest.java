package io.quarkus.runtime.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.LogRecord;

import org.jboss.logmanager.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LogMetricsHandlerTest {

    LogMetricsHandler handler;

    NavigableMap<Integer, LongAdder> counters;

    @BeforeEach
    public void setUp() {
        counters = new TreeMap<>();
        counters.put(Level.TRACE.intValue(), new LongAdder());
        counters.put(Level.DEBUG.intValue(), new LongAdder());
        counters.put(Level.WARN.intValue(), new LongAdder());
        handler = new LogMetricsHandler(counters);
    }

    @Test
    public void equivalentLevelsShouldBeMerged() {
        handler.publish(new LogRecord(Level.DEBUG, "test"));
        handler.publish(new LogRecord(Level.FINE, "test"));

        assertEquals(2, counters.get(Level.DEBUG.intValue()).sum());
    }

    @Test
    public void shouldFilterRecords() {
        handler.setLevel(Level.INFO);

        handler.publish(new LogRecord(Level.DEBUG, "test"));

        assertEquals(0, counters.get(Level.DEBUG.intValue()).sum());
    }

    @Test
    public void nonStandardLevelShouldBeCountedWithLowerStandardLevel() {
        handler.publish(new LogRecord(new CustomLevel("IMPORTANT", 950), "test"));

        assertEquals(1, counters.get(Level.WARN.intValue()).sum());
    }

    @Test
    public void anythingBelowTraceShouldBeCountedAsTrace() {
        handler.publish(new LogRecord(Level.FINEST, "test"));

        assertEquals(1, counters.get(Level.TRACE.intValue()).sum());
    }

    static class CustomLevel extends java.util.logging.Level {
        CustomLevel(final String name, final int value) {
            super(name, value);
        }
    }
}
