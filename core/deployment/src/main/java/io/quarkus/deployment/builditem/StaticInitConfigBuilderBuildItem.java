package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Provides a way to register a {@link io.quarkus.runtime.configuration.ConfigBuilder} in STATIC INIT.
 */
public final class StaticInitConfigBuilderBuildItem extends MultiBuildItem {
    private final String builderClassName;

    public StaticInitConfigBuilderBuildItem(final String builderClassName) {
        this.builderClassName = builderClassName;
    }

    public StaticInitConfigBuilderBuildItem(final Class<?> builderClass) {
        this(builderClass.getName());
    }

    public String getBuilderClassName() {
        return builderClassName;
    }
}
