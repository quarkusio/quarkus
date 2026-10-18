package io.quarkus.undertow.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class IgnoredServletContainerInitializerBuildItem extends MultiBuildItem {

    final String sciClass;

    public IgnoredServletContainerInitializerBuildItem(String sciClass) {
        this.sciClass = sciClass;
    }

    public String getSciClass() {
        return sciClass;
    }
}
