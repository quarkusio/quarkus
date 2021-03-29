
package io.quarkus.container.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class AvailableContainerImageExtensionBuildItem extends MultiBuildItem {

    private final String name;

    public AvailableContainerImageExtensionBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

}
