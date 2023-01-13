package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Provides a way to register a {@link io.quarkus.runtime.configuration.ConfigBuilder} in RUNTIME.
 */
public final class RunTimeConfigBuilderBuildItem extends MultiBuildItem {
    private final String builderClassName;

    public RunTimeConfigBuilderBuildItem(final String builderClassName) {
        this.builderClassName = builderClassName;
    }

    public RunTimeConfigBuilderBuildItem(final Class<?> builderClass) {
        this(builderClass.getName());
    }

    public String getBuilderClassName() {
        return builderClassName;
    }
}
