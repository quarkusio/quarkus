package io.quarkus.it.opentelemetry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@EnabledIfSystemProperty(named = "enable-db2", matches = "true")
@QuarkusTest
@QuarkusTestResource(value = Db2LifecycleManager.class, restrictToAnnotatedClass = true)
public class Db2OpenTelemetryJdbcInstrumentationTest extends OpenTelemetryJdbcInstrumentationTest {

    @Test
    void testDb2SqlQueryTraced() {
        testQueryTraced("db2", "Db2Hit");
    }

}
