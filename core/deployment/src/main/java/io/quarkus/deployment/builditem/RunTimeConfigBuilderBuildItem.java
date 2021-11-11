package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

public final class RunTimeConfigBuilderBuildItem extends MultiBuildItem {
    private String builderClassName;

    public RunTimeConfigBuilderBuildItem(final String builderClassName) {
        this.builderClassName = builderClassName;
    }

    public String getBuilderClassName() {
        return builderClassName;
    }
}
