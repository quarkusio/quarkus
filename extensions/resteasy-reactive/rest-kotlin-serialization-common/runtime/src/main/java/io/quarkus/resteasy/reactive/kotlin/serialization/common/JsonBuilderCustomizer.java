package io.quarkus.resteasy.reactive.kotlin.serialization.common;

import kotlinx.serialization.json.JsonBuilder;

/**
 * Meant to be implemented by a CDI bean that provides arbitrary customization for {@link JsonBuilder} that is built up by
 * Quarkus.
 * <p>
 * All implementations (that are registered as CDI beans) are taken into account when producing the default
 * {@link JsonBuilder}.
 */
public interface JsonBuilderCustomizer extends Comparable<JsonBuilderCustomizer> {

    int DEFAULT_PRIORITY = 0;

    void customize(JsonBuilder jsonBuilder);

    /**
     * Defines the priority that the customizers are applied.
     * A lower integer value means that the customizer will be applied after a customizer with a higher priority
     */
    default int priority() {
        return DEFAULT_PRIORITY;
    }

    default int compareTo(JsonBuilderCustomizer o) {
        return Integer.compare(o.priority(), priority());
    }
}
