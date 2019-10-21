package io.quarkus.creator.phase.runnerjar.test;

import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This tests the core logic of the variable substitution;
 */
public class VariableSubstitutionTest {
    private static final String PLACEHOLDER_PREFIX = "${";
    private static final String PLACEHOLDER_SUFFIX = "}";

    @Test
    public void testVariableSubstitution() {
        System.setProperty("env.TEST", "TEST");
        String trimmedValue = "${env.TEST}";
        if (!trimmedValue.isEmpty() && trimmedValue.startsWith(PLACEHOLDER_PREFIX) &&
                trimmedValue.endsWith(PLACEHOLDER_SUFFIX)) {

            String variableName = trimmedValue.substring(PLACEHOLDER_PREFIX.length(),
                    trimmedValue.length() - PLACEHOLDER_SUFFIX.length());
            assertNotNull(variableName);
            assertEquals("env.TEST", variableName);
            String systemPropertyValue = System.getProperty(variableName);
            if (Objects.nonNull(systemPropertyValue) && !systemPropertyValue.trim().isEmpty()) {
                assertEquals("TEST", systemPropertyValue);
            }
        }
    }
}
