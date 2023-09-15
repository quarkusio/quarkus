package io.quarkus.it.opentelemetry.quartz;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class OpenTelemetryQuartzIT extends OpenTelemetryQuartzTest {
    @Test
    @DisabledOnIntegrationTest("native mode testing span does not have a field 'exception' (only in integration-test, not in quarkus app)")
    @Override
    public void quartzSpanTest() {
        super.quartzSpanTest();
    }
}
