package io.quarkus.undertow.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class BlacklistedServletContainerInitializerBuildItem extends MultiBuildItem {

    final String sciClass;

    public BlacklistedServletContainerInitializerBuildItem(String sciClass) {
        this.sciClass = sciClass;
    }

    public String getSciClass() {
        return sciClass;
    }
}
