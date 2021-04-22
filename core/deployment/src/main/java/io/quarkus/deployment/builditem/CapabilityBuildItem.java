package io.quarkus.deployment.builditem;

import java.util.Objects;

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
    private final String provider;

    /**
     * @deprecated in favor of {@link #CapabilityBuildItem(String, String))} that also accepts the provider
     *             of the capability to be highlighted in the error messages in case of detected capability conflicts.
     *
     * @param name capability name
     */
    @Deprecated
    public CapabilityBuildItem(String name) {
        this(name, "<unknown>");
    }

    /**
     * @param name capability name
     * @param provider capability provider
     */
    public CapabilityBuildItem(String name, String provider) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.provider = Objects.requireNonNull(provider, "provider cannot be null");
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }
}
