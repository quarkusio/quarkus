package io.quarkus.arc.deployment;

import io.quarkus.arc.ArcContainer;
import io.quarkus.builder.item.SimpleBuildItem;

final class ArcContainerBuildItem extends SimpleBuildItem {

    private final ArcContainer container;

    ArcContainerBuildItem(ArcContainer container) {
        this.container = container;
    }

    public ArcContainer getContainer() {
        return container;
    }

}
