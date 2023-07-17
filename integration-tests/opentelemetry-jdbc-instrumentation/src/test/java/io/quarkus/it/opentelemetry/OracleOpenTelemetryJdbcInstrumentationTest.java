package io.quarkus.it.opentelemetry;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = OracleLifecycleManager.class, restrictToAnnotatedClass = true)
public class OracleOpenTelemetryJdbcInstrumentationTest extends OpenTelemetryJdbcInstrumentationTest {

    @Test
    void testOracleQueryTraced() {
        testQueryTraced("oracle", "OracleHit");
    }

}
