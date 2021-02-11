package io.quarkus.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;

/**
 * This build items holds the set of registered capabilities.
 * 
 * @see CapabilityBuildItem
 */
public final class Capabilities extends SimpleBuildItem {

    private final Set<String> capabilities;

    public Capabilities(Set<String> capabilities) {
        this.capabilities = capabilities;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    public boolean isCapabilityPresent(String capability) {
        return isPresent(capability);
    }

    public boolean isPresent(Capability capability) {
        return isPresent(capability.getName());
    }

    public boolean isPresent(String capability) {
        return capabilities.contains(capability);
    }

    public boolean isMissing(String capability) {
        return !isPresent(capability);
    }

    public boolean isMissing(Capability capability) {
        return isMissing(capability.getName());
    }

}
