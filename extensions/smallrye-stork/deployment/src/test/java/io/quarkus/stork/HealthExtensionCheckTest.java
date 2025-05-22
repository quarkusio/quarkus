package io.quarkus.stork;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@DisabledOnOs(OS.WINDOWS)
@QuarkusTestResource(ConsulContainerWithFixedPortsTestResource.class)
public class HealthExtensionCheckTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.INFO.intValue())
            .setForcedDependencies(
                    Arrays.asList(
                            Dependency.of("io.quarkus", "quarkus-smallrye-stork", Version.getVersion()),
                            Dependency.of("io.quarkus", "quarkus-smallrye-health", Version.getVersion()),
                            Dependency.of("io.smallrye.stork", "stork-service-registration-consul", "2.7.4")))
            .assertLogRecords(logRecords -> {
                List<LogRecord> logs = logRecords.stream()
                        .filter(l -> l.getMessage().contains("Using Smallrye Health Check defaults: %s"))
                        .filter(l -> l.getParameters() != null && Arrays.asList(l.getParameters()).contains("/q/health/live"))
                        .toList();

                assertEquals(1, logs.size());
            });

    @Test
    void shouldUseSmallryeHealthCheck() {

    }
}
