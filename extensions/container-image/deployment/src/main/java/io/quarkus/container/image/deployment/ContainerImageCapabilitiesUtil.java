package io.quarkus.container.image.deployment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.deployment.Capabilities;

public final class ContainerImageCapabilitiesUtil {

    public final static Map<String, String> CAPABILITY_TO_EXTENSION_NAME = new HashMap<>();
    static {
        CAPABILITY_TO_EXTENSION_NAME.put(Capabilities.CONTAINER_IMAGE_JIB, "quarkus-container-image-jib");
        CAPABILITY_TO_EXTENSION_NAME.put(Capabilities.CONTAINER_IMAGE_DOCKER, "quarkus-container-image-docker");
        CAPABILITY_TO_EXTENSION_NAME.put(Capabilities.CONTAINER_IMAGE_S2I, "quarkus-container-image-s2i");
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
                    + " were detected, at most one container-image extension can be present. ");
        }
        return activeContainerImageCapabilities.isEmpty() ? Optional.empty()
                : Optional.of(activeContainerImageCapabilities.iterator().next());
    }

    private static Set<String> getContainerImageCapabilities(Capabilities capabilities) {
        Set<String> activeContainerImageCapabilities = new HashSet<>();
        for (String capability : capabilities.getCapabilities()) {
            if (capability.toLowerCase().contains("container-image")) {
                if (!CAPABILITY_TO_EXTENSION_NAME.containsKey(capability)) {
                    throw new IllegalArgumentException("Unknown container-image capability: " + capability);
                }
                activeContainerImageCapabilities.add(CAPABILITY_TO_EXTENSION_NAME.get(capability));
            }
        }
        return activeContainerImageCapabilities;
    }
}
