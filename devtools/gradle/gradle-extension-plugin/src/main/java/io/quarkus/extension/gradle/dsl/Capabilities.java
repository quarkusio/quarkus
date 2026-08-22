package io.quarkus.extension.gradle.dsl;

import java.util.ArrayList;
import java.util.List;

/**
 * Declares capabilities provided or required by a Quarkus extension.
 * <p>
 * Configure this block through {@code quarkusExtension.capabilities}. Each declaration returns its
 * {@link Capability} so optional conditions can be attached fluently.
 */
public class Capabilities {

    private List<Capability> provided = new ArrayList<>(0);
    private List<Capability> required = new ArrayList<>(0);

    /**
     * Adds a capability provided by the extension.
     *
     * @param name the capability name
     * @return the new capability declaration
     */
    public Capability provides(String name) {
        Capability capability = new Capability(name);
        provided.add(capability);
        return capability;
    }

    /**
     * Adds a capability required by the extension.
     *
     * @param name the capability name
     * @return the new capability declaration
     */
    public Capability requires(String name) {
        Capability capability = new Capability(name);
        required.add(capability);
        return capability;
    }

    /**
     * Returns the provided capability declarations in declaration order.
     *
     * @return the mutable provided-capability declarations
     */
    public List<Capability> getProvidedCapabilities() {
        return provided;
    }

    /**
     * Returns the required capability declarations in declaration order.
     *
     * @return the mutable required-capability declarations
     */
    public List<Capability> getRequiredCapabilities() {
        return required;
    }
}
