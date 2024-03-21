package io.quarkus.opentelemetry.deployment.traces;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class OpenTelemetryJdbcInstrumentationValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "quarkus.datasource.db-kind=h2\n" +
                                    "quarkus.otel.metrics.exporter=none\n" +
                                    "quarkus.datasource.jdbc.driver=io.opentelemetry.instrumentation.jdbc.OpenTelemetryDriver\n"),
                            "application.properties"))
            .assertException(t -> {
                Throwable e = t;
                ConfigurationException te = null;
                while (e != null) {
                    if (e instanceof ConfigurationException) {
                        te = (ConfigurationException) e;
                        break;
                    }
                    e = e.getCause();
                }
                if (te == null) {
                    fail("No configuration exception thrown: " + t);
                }
                assertTrue(te.getMessage().contains("Data source '<default>' is using unsupported JDBC driver"),
                        te.getMessage());
                assertTrue(te.getMessage().contains("io.opentelemetry.instrumentation.jdbc.OpenTelemetryDriver"),
                        te.getMessage());
                assertTrue(te.getMessage().contains("quarkus.datasource.jdbc.telemetry"), te.getMessage());
            });

    @Test
    public void testValidation() {
        fail();
    }

}
