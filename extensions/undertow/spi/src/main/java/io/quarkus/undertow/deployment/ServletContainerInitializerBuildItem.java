package io.quarkus.undertow.deployment;

import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

public final class ServletContainerInitializerBuildItem extends MultiBuildItem {

    final String sciClass;
    final Set<String> handlesTypes;

    public ServletContainerInitializerBuildItem(String sciClass, Set<String> handlesTypes) {
        this.sciClass = sciClass;
        this.handlesTypes = handlesTypes;
    }

    public String getSciClass() {
        return sciClass;
    }

    public Set<String> getHandlesTypes() {
        return handlesTypes;
    }
}
