package io.quarkus.it.picocli;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
@TestProfile(JsonLoggingTest.JsonLoggingProfile.class)
public class JsonLoggingTest {

    @Test
    public void testJsonLoggingOutput(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("json-logging", "world");
        assertThat(result.exitCode()).isZero();
        String output = result.getOutput().trim();
        assertThat(output).startsWith("{");
        assertThat(output).contains("Hello world");
    }

    public static class JsonLoggingProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.log.console.json.enabled", "true",
                    "quarkus.log.level", "INFO");
        }
    }
}
