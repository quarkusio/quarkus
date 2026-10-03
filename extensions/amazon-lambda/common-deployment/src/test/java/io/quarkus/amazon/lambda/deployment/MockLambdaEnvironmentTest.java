package io.quarkus.amazon.lambda.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

class MockLambdaEnvironmentTest {

    @Test
    void derivesDefaultsFromArtifactId() {
        MockLambdaEnvironment.Values values = MockLambdaEnvironment.fromBuildSystemProperties(
                Map.of("quarkus.lambda.mock-event-server.enabled", "true"),
                "my-lambda-app");

        assertEquals("MyLambdaApp", values.functionName());
        assertEquals("$LATEST", values.functionVersion());
        assertEquals("128", values.functionMemorySize());
        assertEquals("/aws/lambda/MyLambdaApp", values.logGroupName());
        assertEquals("local/dev", values.logStreamName());
    }

    @Test
    void respectsConfiguredOverrides() {
        MockLambdaEnvironment.Values values = MockLambdaEnvironment.fromBuildSystemProperties(
                Map.of(
                        "quarkus.lambda.mock-event-server.enabled", "true",
                        "quarkus.lambda.mock-environment.function-name", "custom-function",
                        "quarkus.lambda.mock-environment.function-version", "42",
                        "quarkus.lambda.mock-environment.function-memory-size", "512",
                        "quarkus.lambda.mock-environment.log-group-name", "/custom/log-group",
                        "quarkus.lambda.mock-environment.log-stream-name", "custom-stream"),
                "ignored");

        assertEquals("custom-function", values.functionName());
        assertEquals("42", values.functionVersion());
        assertEquals("512", values.functionMemorySize());
        assertEquals("/custom/log-group", values.logGroupName());
        assertEquals("custom-stream", values.logStreamName());
    }

    @Test
    void disabledWhenMockEventServerDisabled() {
        assertNull(MockLambdaEnvironment.fromBuildSystemProperties(
                Map.of("quarkus.lambda.mock-event-server.enabled", "false"),
                "my-app"));
    }
}
