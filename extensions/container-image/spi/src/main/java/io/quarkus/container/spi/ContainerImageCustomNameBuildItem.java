package io.quarkus.container.spi;

import io.quarkus.builder.item.BuildItem;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * This {@link BuildItem} can be used to override the default image name. It can be used in cases where the name of the
 * image is customized externally. Example: The openshift extension may override the name. To ensure that things are in
 * sync with the image name needs to be set.
 */
public final class ContainerImageCustomNameBuildItem extends SimpleBuildItem {

    private final String name;

    public ContainerImageCustomNameBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
