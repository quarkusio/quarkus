package io.quarkus.gradle.application.internal.plugin;

import java.util.Map;

import org.gradle.api.tasks.testing.Test;

final class QuarkusApplicationIntegrationTestLaunchDefaults {

    private static final String MANAGEMENT_PORT = "quarkus.management.port";
    private static final String MANAGEMENT_TEST_PORT = "quarkus.management.test-port";

    private QuarkusApplicationIntegrationTestLaunchDefaults() {
    }

    static void configure(Test test) {
        Map<String, Object> systemProperties = test.getSystemProperties();
        if (!systemProperties.containsKey(MANAGEMENT_PORT) && !systemProperties.containsKey(MANAGEMENT_TEST_PORT)) {
            systemProperties.put(MANAGEMENT_PORT, "0");
        }
    }
}
