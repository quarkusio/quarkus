package io.quarkus.qute.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class GeneratedValueResolverBuildItem extends MultiBuildItem {

    private final String className;

    public GeneratedValueResolverBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

}
