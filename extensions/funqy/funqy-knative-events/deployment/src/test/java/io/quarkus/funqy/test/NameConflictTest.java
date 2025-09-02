package io.quarkus.funqy.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NameConflictTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(NameConflict.class))
            .setLogRecordPredicate(logRecord -> logRecord.getLevel().intValue() >= Level.WARNING.intValue() &&
                    logRecord.getLoggerName().startsWith("io.quarkus.funqy"))
            .assertLogRecords(logRecords -> {
                boolean match = logRecords
                        .stream()
                        .anyMatch(logRecord -> logRecord.getMessage().contains("Name conflict"));
                if (!match) {
                    fail("Log does not contain message about name conflict.");
                }
            });

    @Test
    void test() {
        assertTrue(true);
    }
}
