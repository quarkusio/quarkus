package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A generated wiring class
 */
public final class WiringClassBuildItem extends MultiBuildItem {

    private final String name;
    private final byte[] data;

    public WiringClassBuildItem(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }
}
