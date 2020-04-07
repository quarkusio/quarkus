package io.quarkus.container.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A label to be added to the built container image
 * This will only have an effect if the extension building the container image
 * supports adding custom labels (like the Jib extension)
 */
public final class ContainerImageLabelBuildItem extends MultiBuildItem {

    private final String name;
    private final String value;

    public ContainerImageLabelBuildItem(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
