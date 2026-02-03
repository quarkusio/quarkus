package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that represents that result of building an AOT enhanced container image
 */
public final class BuildAotOptimizedContainerImageResultBuildItem extends SimpleBuildItem {

    private final String containerImage;

    public BuildAotOptimizedContainerImageResultBuildItem(String containerImage) {
        this.containerImage = containerImage;
    }

    public String getContainerImage() {
        return containerImage;
    }
}
