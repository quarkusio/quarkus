package io.quarkus.qute.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Holds a name of a generated {@link io.quarkus.qute.ValueResolver} class.
 */
public final class GeneratedValueResolverBuildItem extends MultiBuildItem {

    private final String className;

    public GeneratedValueResolverBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

}
