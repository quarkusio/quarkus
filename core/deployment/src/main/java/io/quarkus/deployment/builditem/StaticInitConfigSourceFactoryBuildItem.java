package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Provides a way to register a ConfigSourceFactory in STATIC INIT.
 *
 * @deprecated Please use {@link StaticInitConfigBuilderBuildItem} instead.
 */
@Deprecated
public final class StaticInitConfigSourceFactoryBuildItem extends MultiBuildItem {
    private String factoryClassName;

    public StaticInitConfigSourceFactoryBuildItem(final String factoryClassName) {
        this.factoryClassName = factoryClassName;
    }

    public String getFactoryClassName() {
        return factoryClassName;
    }
}
