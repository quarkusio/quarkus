package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

public final class StaticInitConfigBuilderBuildItem extends MultiBuildItem {
    private String builderClassName;

    public StaticInitConfigBuilderBuildItem(final String builderClassName) {
        this.builderClassName = builderClassName;
    }

    public String getBuilderClassName() {
        return builderClassName;
    }
}
