package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;

/**
 * Represents a technical capability that can be queried by other extensions.
 * <p>
 * Build steps can inject {@link Capabilities} - a convenient build item that holds the set of registered capabilities.
 * <p>
 * An extension may provide multiple capabilities. Multiple extensions can provide the same capability. By default, capabilities
 * are not displayed to users.
 * <p>
 * Capabilities should follow the naming conventions of Java packages; e.g. {@code io.quarkus.security.jpa}. Capabilities
 * provided by core extensions should be listed in the {@link Capability} enum and their name should always start with the
 * {@code io.quarkus} prefix.
 * 
 * @see Capabilities
 * @see Capability
 */
public final class CapabilityBuildItem extends MultiBuildItem {

    private final String name;

    public CapabilityBuildItem(Capability capability) {
        this(capability.getName());
    }

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
