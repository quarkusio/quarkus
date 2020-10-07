package io.quarkus.rest.spi;

import io.quarkus.builder.item.MultiBuildItem;

// TODO: we should really provide a better alternative than this...
public final class DynamicFeatureBuildItem extends MultiBuildItem {

    private final String className;

    public DynamicFeatureBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
