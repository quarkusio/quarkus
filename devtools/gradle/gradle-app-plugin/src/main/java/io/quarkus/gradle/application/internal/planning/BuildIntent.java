package io.quarkus.gradle.application.internal.planning;

import java.util.Map;

public record BuildIntent(Map<String, String> forcedProperties) {
    public BuildIntent {
        forcedProperties = Map.copyOf(forcedProperties);
    }
}
