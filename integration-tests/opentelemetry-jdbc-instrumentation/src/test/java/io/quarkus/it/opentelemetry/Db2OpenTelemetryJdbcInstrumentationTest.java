package io.quarkus.it.opentelemetry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;

@EnabledIfSystemProperty(named = "enable-db2", matches = "true")
@QuarkusTest
@WithTestResource(Db2LifecycleManager.class)
public class Db2OpenTelemetryJdbcInstrumentationTest extends OpenTelemetryJdbcInstrumentationTest {

    @Test
    void testDb2SqlQueryTraced() {
        testQueryTraced("db2", "Db2Hit");
    }

}
