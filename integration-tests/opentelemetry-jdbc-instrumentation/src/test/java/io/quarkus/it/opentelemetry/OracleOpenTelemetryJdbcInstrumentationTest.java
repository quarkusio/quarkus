package io.quarkus.it.opentelemetry;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(OracleOpenTelemetryJdbcInstrumentationTest.OracleTestProfile.class)
public class OracleOpenTelemetryJdbcInstrumentationTest extends OpenTelemetryJdbcInstrumentationTest {

    @Test
    void testOracleQueryTraced() {
        testQueryTraced("oracle", "OracleHit");
    }

    public static class OracleTestProfile implements QuarkusTestProfile {

        @Override
        public String getConfigProfile() {
            return "oracle-profile";
        }
    }

}
