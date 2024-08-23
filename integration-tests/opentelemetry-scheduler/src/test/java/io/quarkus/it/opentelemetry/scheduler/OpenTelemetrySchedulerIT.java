package io.quarkus.it.opentelemetry.scheduler;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class OpenTelemetrySchedulerIT extends OpenTelemetrySchedulerTest {
    @Test
    @DisabledOnIntegrationTest("native mode testing span does not have a field 'exception' (only in integration-test, not in quarkus app)")
    @Override
    public void schedulerSpanTest() {
        super.schedulerSpanTest();
    }
}
