package io.quarkus.container.image.deployment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;

public final class ContainerImageCapabilitiesUtil {

    public final static Map<String, String> CAPABILITY_TO_EXTENSION_NAME = new HashMap<>();
    static {
        CAPABILITY_TO_EXTENSION_NAME.put(Capability.CONTAINER_IMAGE_JIB, "quarkus-container-image-jib");
        CAPABILITY_TO_EXTENSION_NAME.put(Capability.CONTAINER_IMAGE_DOCKER, "quarkus-container-image-docker");
        CAPABILITY_TO_EXTENSION_NAME.put(Capability.CONTAINER_IMAGE_S2I, "quarkus-container-image-s2i");
        CAPABILITY_TO_EXTENSION_NAME.put(Capability.CONTAINER_IMAGE_OPENSHIFT, "quarkus-container-image-openshift");
    }

    private ContainerImageCapabilitiesUtil() {
    }

    /**
     * Returns the active container image capability or throws an {@code IllegalStateException} if more than one are active
     */
    public static Optional<String> getActiveContainerImageCapability(Capabilities capabilities) {
        Set<String> activeContainerImageCapabilities = ContainerImageCapabilitiesUtil
                .getContainerImageCapabilities(capabilities);
        if (activeContainerImageCapabilities.size() > 1) {
            throw new IllegalStateException(String.join(" and ", activeContainerImageCapabilities)
                    + " were detected, at most one container-image extension can be present.\n"
                    + "Either remove the unneeded ones, or select one by adding the property 'quarkus.container-image.builder=<extension name (without the `container-image-` prefix)>' in application.properties or as a system property.");
        }
        return activeContainerImageCapabilities.isEmpty() ? Optional.empty()
                : Optional.of(activeContainerImageCapabilities.iterator().next());
    }

    private static Set<String> getContainerImageCapabilities(Capabilities capabilities) {
        Set<String> activeContainerImageCapabilities = new HashSet<>();
        for (String capability : capabilities.getCapabilities()) {
            if (capability.toLowerCase().contains("container.image")) {
                if (!CAPABILITY_TO_EXTENSION_NAME.containsKey(capability)) {
                    throw new IllegalArgumentException("Unknown container image capability: " + capability);
                }
                activeContainerImageCapabilities.add(CAPABILITY_TO_EXTENSION_NAME.get(capability));
            }
        }
        return activeContainerImageCapabilities;
    }
}
