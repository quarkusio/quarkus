package io.quarkus.jackson;

import tools.jackson.core.json.JsonFactoryBuilder;

public interface JsonFactoryBuilderCustomizer extends Comparable<JsonFactoryBuilderCustomizer> {

    int MINIMUM_PRIORITY = Integer.MIN_VALUE;
    int MAXIMUM_PRIORITY = Integer.MAX_VALUE;
    int DEFAULT_PRIORITY = 0;

    void customize(JsonFactoryBuilder builder);

    /**
     * Defines the priority that the customizers are applied.
     * A lower integer value means that the customizer will be applied after a customizer with a higher priority
     */
    default int priority() {
        return DEFAULT_PRIORITY;
    }

    default int compareTo(JsonFactoryBuilderCustomizer o) {
        return Integer.compare(o.priority(), priority());
    }
}
