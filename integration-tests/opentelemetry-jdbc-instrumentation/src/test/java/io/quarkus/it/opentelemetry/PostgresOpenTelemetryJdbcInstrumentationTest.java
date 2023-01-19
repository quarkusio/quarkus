package io.quarkus.it.opentelemetry;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = PostgreSqlLifecycleManager.class, restrictToAnnotatedClass = true)
public class PostgresOpenTelemetryJdbcInstrumentationTest extends OpenTelemetryJdbcInstrumentationTest {

    @Test
    void testPostgreSqlQueryTraced() {
        testQueryTraced("postgresql", "PgHit");
    }

}
