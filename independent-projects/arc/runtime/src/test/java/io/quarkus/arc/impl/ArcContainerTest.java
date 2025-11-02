package io.quarkus.arc.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;

public class ArcContainerTest {

    @Test
    public void testContainer() {
        InMemoryLogHandler handler = new InMemoryLogHandler();
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(handler);
        try {
            System.clearProperty("quarkus.arc.log-no-container");
            // By default, no action is performed
            assertNull(Arc.container());
            assertTrue(handler.records.isEmpty());

            IllegalStateException ise = assertThrows(IllegalStateException.class, () -> Arc.requireContainer());
            assertTrue(ise.getMessage().contains("ArC container not initialized"));

            // quarkus.arc.log-no-containe=true -> log a warning
            System.setProperty("quarkus.arc.log-no-container", "true");
            assertNull(Arc.container());
            List<LogRecord> records = handler.getRecords();
            assertEquals(1, records.size());
            LogRecord record = records.get(0);
            assertTrue(record.getMessage().contains("ArC: container not initialized"));
        } finally {
            System.clearProperty("quarkus.arc.log-no-container");
            handler.clearRecords();
            rootLogger.removeHandler(handler);
        }
    }

}
