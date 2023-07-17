package io.quarkus.qute.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class GeneratedTemplateInitializerBuildItem extends MultiBuildItem {

    private final String className;

    public GeneratedTemplateInitializerBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

}
