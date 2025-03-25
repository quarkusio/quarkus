package io.quarkus.it.opentelemetry;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(value = OracleLifecycleManager.class, restrictToAnnotatedClass = true)
// TODO See https://github.com/quarkusio/quarkus/issues/46542; this profile should not be needed
@TestProfile(OracleOpenTelemetryJdbcInstrumentationTest.SomeProfile.class)
public class OracleOpenTelemetryJdbcInstrumentationTest extends OpenTelemetryJdbcInstrumentationTest {

    @Test
    void testOracleQueryTraced() {
        testQueryTraced("oracle", "OracleHit");
    }

    public static class SomeProfile implements QuarkusTestProfile {
        public SomeProfile() {
        }
    }
}
