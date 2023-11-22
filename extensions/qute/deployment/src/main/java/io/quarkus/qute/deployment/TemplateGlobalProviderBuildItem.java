package io.quarkus.qute.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class TemplateGlobalProviderBuildItem extends MultiBuildItem {

    private final String className;

    public TemplateGlobalProviderBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

}
