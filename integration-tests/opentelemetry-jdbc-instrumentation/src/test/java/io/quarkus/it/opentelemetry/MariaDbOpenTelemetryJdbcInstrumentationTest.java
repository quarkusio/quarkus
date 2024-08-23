package io.quarkus.it.opentelemetry;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithTestResource(MariaDbLifecycleManager.class)
public class MariaDbOpenTelemetryJdbcInstrumentationTest extends OpenTelemetryJdbcInstrumentationTest {

    @Test
    void testMariaDbQueryTraced() {
        testQueryTraced("mariadb", "MariaDbHit");
    }

}
