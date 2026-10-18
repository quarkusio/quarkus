package io.quarkus.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class DeprecatedPropertiesTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(logRecords -> {
                List<LogRecord> deprecatedProperties = logRecords.stream()
                        .filter(l -> l.getMessage().contains("Deprecated configuration property"))
                        .toList();

                assertEquals(2, deprecatedProperties.size());
                assertEquals("quarkus.mapping.bt.deprecated", deprecatedProperties.get(0).getParameters()[0]);
                assertTrue(deprecatedProperties.get(0).getParameters()[1].toString().contains("application.properties"));
                assertEquals("quarkus.mapping.rt.deprecated", deprecatedProperties.get(1).getParameters()[0]);
                assertTrue(deprecatedProperties.get(0).getParameters()[1].toString().contains("application.properties"));
            });

    @Test
    void deprecatedProperties() {

    }
}
