package io.quarkus.it.opentelemetry;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@EnabledIfSystemProperty(named = "enable-db2", matches = "true")
@QuarkusIntegrationTest
public class Db2OpenTelemetryJdbcInstrumentationIT extends Db2OpenTelemetryJdbcInstrumentationTest {

}
