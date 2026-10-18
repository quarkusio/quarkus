package io.quarkus.container.image.deployment;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;

public final class ContainerImageCapabilitiesUtil {

    public final static Map<String, String> CAPABILITY_TO_EXTENSION_NAME = Map.of(
            Capability.CONTAINER_IMAGE_JIB, "quarkus-container-image-jib",
            Capability.CONTAINER_IMAGE_DOCKER, "quarkus-container-image-docker",
            Capability.CONTAINER_IMAGE_PODMAN, "quarkus-container-image-podman",
            Capability.CONTAINER_IMAGE_OPENSHIFT, "quarkus-container-image-openshift",
            Capability.CONTAINER_IMAGE_BUILDPACK, "quarkus-container-image-buildpack");

    private final static Map<String, String> CAPABILITY_TO_BUILDER_NAME = Map.of(
            Capability.CONTAINER_IMAGE_JIB, "jib",
            Capability.CONTAINER_IMAGE_DOCKER, "docker",
            Capability.CONTAINER_IMAGE_PODMAN, "podman",
            Capability.CONTAINER_IMAGE_OPENSHIFT, "openshift",
            Capability.CONTAINER_IMAGE_BUILDPACK, "buildpack");

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
                    + "Either remove the unneeded ones, or select one by setting any of the following configuration properties: "
                    + createBuilderSelectionPropertySuggestion(capabilities));
        }
        return activeContainerImageCapabilities.isEmpty() ? Optional.empty()
                : Optional.of(activeContainerImageCapabilities.iterator().next());
    }

    private static StringBuilder createBuilderSelectionPropertySuggestion(Capabilities capabilities) {
        StringBuilder suggestion = new StringBuilder();
        boolean isFirst = true;
        for (String capability : capabilities.getCapabilities()) {
            if (!isContainerImageCapability(capability)) {
                continue;
            }
            if (!isFirst) {
                suggestion.append(", ");
            }
            isFirst = false;
            suggestion.append('\'').append("quarkus.container-image.builder=")
                    .append(CAPABILITY_TO_BUILDER_NAME.get(capability)).append('\'');
        }
        return suggestion;
    }

    private static Set<String> getContainerImageCapabilities(Capabilities capabilities) {
        Set<String> activeContainerImageCapabilities = new HashSet<>();
        for (String capability : capabilities.getCapabilities()) {
            if (isContainerImageCapability(capability)) {
                if (!CAPABILITY_TO_EXTENSION_NAME.containsKey(capability)) {
                    throw new IllegalArgumentException("Unknown container image capability: " + capability);
                }
                activeContainerImageCapabilities.add(CAPABILITY_TO_EXTENSION_NAME.get(capability));
            }
        }
        return activeContainerImageCapabilities;
    }

    private static boolean isContainerImageCapability(String capability) {
        return capability.toLowerCase().contains("container.image");
    }
}
