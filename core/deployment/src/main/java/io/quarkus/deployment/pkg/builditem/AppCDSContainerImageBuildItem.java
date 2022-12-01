package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Indicates that a specific container image should be used to generate the AppCDS file
 */
public final class AppCDSContainerImageBuildItem extends SimpleBuildItem {

    private final String containerImage;

    public AppCDSContainerImageBuildItem(String containerImage) {
        this.containerImage = containerImage;
    }

    public String getContainerImage() {
        return containerImage;
    }
}
