package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.Capabilities;

/**
 * Registers an internal feature.
 * 
 * @see Capabilities#isCapabilityPresent(String)
 */
public final class CapabilityBuildItem extends MultiBuildItem {

    private final String name;

    public CapabilityBuildItem(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
