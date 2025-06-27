package io.quarkus.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DeprecatedPropertiesTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(logRecords -> {
                List<LogRecord> deprecatedProperties = logRecords.stream()
                        .filter(l -> l.getMessage().contains("config property is deprecated"))
                        .toList();

                assertEquals(2, deprecatedProperties.size());
                assertTrue(deprecatedProperties.get(0).getParameters()[0].toString().contains("quarkus.mapping.bt.deprecated"));
                assertTrue(deprecatedProperties.get(1).getParameters()[0].toString().contains("quarkus.mapping.rt.deprecated"));
            });

    @Test
    void deprecatedProperties() {

    }
}
