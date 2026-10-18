package io.quarkus.it.opentelemetry;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = H2DatabaseTestResource.class, restrictToAnnotatedClass = true, initArgs = {
        @ResourceArg(name = H2DatabaseTestResource.QUARKUS_OTEL_SDK_DISABLED, value = "true")
})
public class H2OpenTelemetryOffJdbcInstrumentationTest extends OpenTelemetryJdbcInstrumentationTest {

    @Test
    void testSqlQueryNotTraced() {
        // When OpenTelemetry is disabled at runtime, the queries should not be traced
        testQueryNotTraced("h2");
    }
}
