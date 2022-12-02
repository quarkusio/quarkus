package io.quarkus.container.spi;

import io.quarkus.builder.item.SimpleBuildItem;

public final class BaseImageInfoBuildItem extends SimpleBuildItem {

    private final String image;

    public BaseImageInfoBuildItem(String image) {
        this.image = image;
    }

    public String getImage() {
        return this.image;
    }

}
