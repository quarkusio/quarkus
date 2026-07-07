package io.quarkus.gradle.application.internal.config;

import java.util.Map;

public record ShapeExpectation(String buildName, String taskName,
        Map<String, String> expectedValues) {

    public ShapeExpectation {
        if (buildName == null || buildName.isBlank()) {
            throw new IllegalArgumentException("Quarkus application shape expectation requires a build name");
        }
        if (taskName == null || taskName.isBlank()) {
            throw new IllegalArgumentException("Quarkus application shape expectation requires a task name");
        }
        expectedValues = Map.copyOf(expectedValues);
    }
}
