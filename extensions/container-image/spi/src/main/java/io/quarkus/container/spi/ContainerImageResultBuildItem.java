package io.quarkus.container.spi;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ContainerImageResultBuildItem extends SimpleBuildItem {

    private final String imageId;
    private final String repository;
    private final String tag;

    public ContainerImageResultBuildItem(String imageId, String repository, String tag) {
        this.imageId = imageId;
        this.repository = repository;
        this.tag = tag;
    }

    public String getImageId() {
        return this.imageId;
    }

    public String getRepository() {
        return this.repository;
    }

    public String getTag() {
        return this.tag;
    }
}
