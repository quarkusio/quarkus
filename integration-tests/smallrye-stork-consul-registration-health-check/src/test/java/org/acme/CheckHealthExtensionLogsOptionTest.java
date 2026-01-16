package org.acme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * A configuration block is defined for a single service (see Map returned by ConsulTestResource), but without the need to
 * indicate the registrar type.
 */

@DisabledOnOs(OS.WINDOWS)
@QuarkusTestResource(ConsulContainerWithFixedPortsTestResource.class)
public class CheckHealthExtensionLogsOptionTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties"))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.FINE.intValue())
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
