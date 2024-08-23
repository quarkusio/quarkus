package io.quarkus.it.opentelemetry;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithTestResource(PostgreSqlLifecycleManager.class)
public class PostgresOpenTelemetryJdbcInstrumentationTest extends OpenTelemetryJdbcInstrumentationTest {

    @Test
    void testPostgreSqlQueryTraced() {
        testQueryTraced("postgresql", "PgHit");
    }

}
