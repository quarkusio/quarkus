
package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ContainerImageBuildItem extends SimpleBuildItem {

    private final String image;

    public ContainerImageBuildItem(String image) {
        this.image = image;
    }

    public String getImage() {
        return this.image;
    }
}
