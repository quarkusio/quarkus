package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that represents that result of building an AOT enhanced container image
 */
public final class BuildAotOptimizedContainerImageResultBuildItem extends MultiBuildItem {

    private final String containerImage;

    public BuildAotOptimizedContainerImageResultBuildItem(String containerImage) {
        this.containerImage = containerImage;
    }

    public String getContainerImage() {
        return containerImage;
    }
}
