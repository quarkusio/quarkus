package io.quarkus.stork;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

public class HealthExtensionCheckTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.INFO.intValue())
            .setForcedDependencies(
                    Arrays.asList(
                            Dependency.of("io.quarkus", "quarkus-smallrye-health", Version.getVersion())))
            .assertLogRecords(logRecords -> {
                List<LogRecord> deprecatedProperties = logRecords.stream()
                        .filter(l -> l.getMessage().contains("Using Smallrye Health Check defaults"))
                        .toList();

                assertEquals(1, deprecatedProperties.size());
            });

    @Test
    void shouldUseSmallryeHealthCheck() {

    }
}
