package io.quarkus.test.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * The command of an extension is executed by an external tool, so the integration test profile and the additional
 * properties reach the application as environment variables.
 */
public class RunCommandLauncherTest {

    @Test
    public void testProfileIsPassedAsEnvironmentVariable() {
        Map<String, String> environment = new HashMap<>();
        RunCommandLauncher.applyEnvironment(environment, "someprofile", Map.of());
        assertEquals("someprofile", environment.get("QUARKUS_PROFILE"));
    }

    @Test
    public void testMissingProfileLeavesTheEnvironmentAlone() {
        Map<String, String> environment = new HashMap<>();
        RunCommandLauncher.applyEnvironment(environment, null, Map.of());
        RunCommandLauncher.applyEnvironment(environment, " ", Map.of());
        assertFalse(environment.containsKey("QUARKUS_PROFILE"));
    }

    @Test
    public void testPropertiesArePassedAsEnvironmentVariables() {
        Map<String, String> environment = new HashMap<>();
        RunCommandLauncher.applyEnvironment(environment, "someprofile",
                Map.of("quarkus.http.test-port", "8083", "quarkus.datasource.\"users\".jdbc.url", "jdbc:h2:mem:users"));
        assertEquals("8083", environment.get("QUARKUS_HTTP_TEST_PORT"));
        assertEquals("jdbc:h2:mem:users", environment.get("QUARKUS_DATASOURCE__USERS__JDBC_URL"));
    }

}
