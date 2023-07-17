package io.quarkus.it.opentelemetry;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = MariaDbLifecycleManager.class, restrictToAnnotatedClass = true)
public class MariaDbOpenTelemetryJdbcInstrumentationTest extends OpenTelemetryJdbcInstrumentationTest {

    @Test
    void testMariaDbQueryTraced() {
        testQueryTraced("mariadb", "MariaDbHit");
    }

}
