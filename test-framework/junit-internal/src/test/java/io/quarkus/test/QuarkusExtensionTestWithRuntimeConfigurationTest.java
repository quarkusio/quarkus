package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for QuarkusExtensionTest.withRuntimeConfiguration(String),
 * without starting Quarkus or CDI.
 */
final class QuarkusExtensionTestWithRuntimeConfigurationTest {

    /**
     * Test double that captures calls to overrideRuntimeConfigKey.
     */
    static final class TestableQuarkusExtensionTest extends QuarkusExtensionTest {

        private final Map<String, String> runtimeConfig = new LinkedHashMap<>();

        @Override
        public QuarkusExtensionTest overrideRuntimeConfigKey(String key, String value) {
            runtimeConfig.put(key, value);
            return this;
        }

        Map<String, String> getRuntimeConfig() {
            return runtimeConfig;
        }
    }

    @Test
    void parsesValidConfigBlock() {
        TestableQuarkusExtensionTest testUnit = new TestableQuarkusExtensionTest();

        testUnit.withRuntimeConfiguration("""
                test.inline.runtime=value
                quarkus.log.category.test.category.level=DEBUG
                """);

        Map<String, String> expected = Map.of(
                "test.inline.runtime", "value",
                "quarkus.log.category.test.category.level", "DEBUG");

        assertEquals(expected, testUnit.getRuntimeConfig());
    }

    @Test
    void ignoresBlankAndCommentLines() {
        TestableQuarkusExtensionTest testUnit = new TestableQuarkusExtensionTest();

        testUnit.withRuntimeConfiguration("""
                # comment

                test.key1=foo

                # comment
                test.key2 = bar
                """);

        Map<String, String> expected = Map.of(
                "test.key1", "foo",
                "test.key2", "bar");

        assertEquals(expected, testUnit.getRuntimeConfig());
    }
}
