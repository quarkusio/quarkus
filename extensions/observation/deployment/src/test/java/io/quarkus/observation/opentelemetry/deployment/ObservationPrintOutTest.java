package io.quarkus.observation.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.quarkus.observation.handler.PrintOutHandler;
import io.quarkus.test.QuarkusExtensionTest;

public class ObservationPrintOutTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.observation.print-out", "true")
            .overrideConfigKey("quarkus.otel.traces.exporter", "none")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "none")
            .setLogRecordPredicate(record -> PrintOutHandler.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> assertThat(records)
                    .as("print-out handler records")
                    .anyMatch(record -> record.getMessage().startsWith("START")));

    @Inject
    ObservationRegistry registry;

    @Test
    public void testObservationIsPrintedOut() {
        Observation.start("print-out-test", registry).stop();
    }
}
