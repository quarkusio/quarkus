
package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

public final class DockerImageBuildItem extends SimpleBuildItem {

    private final String image;

    public DockerImageBuildItem(String image) {
        this.image = image;
    }

    public String getImage() {
        return this.image;
    }
}
