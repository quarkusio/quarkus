package io.quarkus.container.image.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Indicates that a container image was built
 */
public final class ContainerImageBuiltBuildItem extends SimpleBuildItem {

    private final String image;

    public ContainerImageBuiltBuildItem(String image) {
        this.image = image;
    }

    public String getImage() {
        return image;
    }
}
