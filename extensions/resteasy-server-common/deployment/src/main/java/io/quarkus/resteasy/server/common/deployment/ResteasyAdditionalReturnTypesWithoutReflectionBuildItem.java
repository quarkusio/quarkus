package io.quarkus.resteasy.server.common.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class ResteasyAdditionalReturnTypesWithoutReflectionBuildItem extends MultiBuildItem {

    private final String className;

    public ResteasyAdditionalReturnTypesWithoutReflectionBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
