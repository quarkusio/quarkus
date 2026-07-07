package io.quarkus.gradle.application.internal.config;

import java.util.Map;

public final class ShapeValidator {

    public void validate(ShapeExpectation expectation, Map<String, String> resolvedValues) {
        for (Map.Entry<String, String> entry : expectation.expectedValues().entrySet()) {
            String resolved = resolvedValues.get(entry.getKey());
            if (!entry.getValue().equals(resolved)) {
                throw new IllegalStateException("Named Quarkus output '" + expectation.buildName()
                        + "' expected " + entry.getKey() + "=" + entry.getValue()
                        + " while executing " + expectation.taskName()
                        + " but resolved " + entry.getKey() + "=" + resolved
                        + ". Descriptor-owned output shape must not be changed by application config.");
            }
        }
    }
}
