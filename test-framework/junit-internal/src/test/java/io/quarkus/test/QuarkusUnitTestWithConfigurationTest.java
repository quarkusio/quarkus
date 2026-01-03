package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for QuarkusUnitTest.withConfiguration(String),
 * without starting Quarkus or CDI.
 */
final class QuarkusUnitTestWithConfigurationTest {

    static final class TestableConfigUnit extends QuarkusUnitTest {

        private final Map<String, String> configMap = new LinkedHashMap<>();

        @Override
        public QuarkusUnitTest overrideConfigKey(String key, String value) {
            configMap.put(key, value);
            return this;
        }

        Map<String, String> getConfig() {
            return configMap;
        }
    }

    @Test
    void parsesConfigurationCorrectly() {
        TestableConfigUnit testUnit = new TestableConfigUnit();

        testUnit.withConfiguration("""
                        quarkus.datasource.db-kind=postgresql
                        quarkus.datasource.username=testuser
                        quarkus.datasource.password=secret
                """);

        Map<String, String> expected = Map.of(
                "quarkus.datasource.db-kind", "postgresql",
                "quarkus.datasource.username", "testuser",
                "quarkus.datasource.password", "secret");

        assertEquals(expected, testUnit.getConfig());
    }

    @Test
    void ignoresBlankLinesAndComments() {
        TestableConfigUnit testUnit = new TestableConfigUnit();

        testUnit.withConfiguration("""
                        # comment

                        my.key = value

                        # another comment
                        other.key= foo
                """);

        Map<String, String> expected = Map.of(
                "my.key", "value",
                "other.key", "foo");

        assertEquals(expected, testUnit.getConfig());
    }
}
