package io.quarkus.it.opentelemetry;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(value = H2DatabaseTestResource.class, restrictToAnnotatedClass = true)
@TestProfile(H2TraceConnectionProfile.class)
public class H2OpenTelemetryTraceConnectionJdbcInstrumentationTest extends OpenTelemetryJdbcInstrumentationTest {

    @Test
    void testH2ConnectionTraced() {
        testConnectionTraced("h2");
    }
}
