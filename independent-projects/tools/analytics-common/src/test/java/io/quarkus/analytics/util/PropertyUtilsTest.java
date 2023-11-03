package io.quarkus.analytics.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
class PropertyUtilsTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    private static Stream<Arguments> getIntTest() {
        return Stream.of(
                arguments("3", null, 1, 3),
                arguments(null, "4", 1, 4),
                arguments("5", "6", 1, 6),
                arguments(null, null, 1, 1));
    }

    private static Stream<Arguments> getStringProperty() {
        return Stream.of(
                arguments("3", null, "1", "3"),
                arguments(null, "4", "1", "4"),
                arguments("5", "6", "1", "6"),
                arguments(null, null, "1", "1"));
    }

    private static Stream<Arguments> getBooleanProperty() {
        return Stream.of(
                arguments("true", null, "false", "true"),
                arguments(null, "true", "false", "true"),
                arguments("true", "false", "false", "false"),
                arguments("false", "true", "false", "true"),
                arguments(null, null, "true", "true"));
    }

    @ParameterizedTest
    @MethodSource
    void getIntTest(String env, String sys, int defaultValue, int expected) {
        if (env != null) {
            environmentVariables.set("TEST_ENV", env);
        }
        if (sys != null) {
            System.setProperty("test.env", sys);
        }
        assertEquals(expected, PropertyUtils.getProperty("test.env", defaultValue));
        System.clearProperty("test.env");
    }

    @ParameterizedTest
    @MethodSource
    void getStringProperty(String env, String sys, String defaultValue, String expected) {
        if (env != null) {
            environmentVariables.set("TEST_ENV", env);
        }
        if (sys != null) {
            System.setProperty("test.env", sys);
        }
        assertEquals(expected, PropertyUtils.getProperty("test.env", defaultValue));
        System.clearProperty("test.env");
    }

    @ParameterizedTest
    @MethodSource
    void getBooleanProperty(String env, String sys, boolean defaultValue, boolean expected) {
        if (env != null) {
            environmentVariables.set("TEST_ENV", env);
        }
        if (sys != null) {
            System.setProperty("test.env", sys);
        }
        assertEquals(expected, PropertyUtils.getProperty("test.env", defaultValue));
        System.clearProperty("test.env");
    }

}
