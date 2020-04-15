package io.quarkus.container.image.deployment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;

public class ContainerProcessor {

    private final static Map<String, String> CAPABILITY_TO_EXTENSION_NAME = new HashMap<>();
    static {
        CAPABILITY_TO_EXTENSION_NAME.put(Capabilities.CONTAINER_IMAGE_JIB, "quarkus-container-image-jib");
        CAPABILITY_TO_EXTENSION_NAME.put(Capabilities.CONTAINER_IMAGE_DOCKER, "quarkus-container-image-docker");
        CAPABILITY_TO_EXTENSION_NAME.put(Capabilities.CONTAINER_IMAGE_S2I, "quarkus-container-image-s2i");
    }

    @BuildStep
    public ContainerImageInfoBuildItem publishImageInfo(ApplicationInfoBuildItem app,
            ContainerImageConfig containerImageConfig, Capabilities capabilities) {

        ensureSingleContainerImageExtension(capabilities);

        return new ContainerImageInfoBuildItem(containerImageConfig.registry,
                containerImageConfig.group,
                containerImageConfig.name.orElse(app.getName()),
                containerImageConfig.tag.orElse(app.getVersion()));
    }

    private void ensureSingleContainerImageExtension(Capabilities capabilities) {
        Set<String> activeContainerImageCapabilities = new HashSet<>();
        for (String capability : capabilities.getCapabilities()) {
            if (capability.toLowerCase().contains("container-image")) {
                if (!CAPABILITY_TO_EXTENSION_NAME.containsKey(capability)) {
                    throw new IllegalArgumentException("Unknown container-image capability: " + capability);
                }
                activeContainerImageCapabilities.add(CAPABILITY_TO_EXTENSION_NAME.get(capability));
            }
        }
        if (activeContainerImageCapabilities.size() > 1) {
            throw new IllegalStateException(String.join(" and ", activeContainerImageCapabilities)
                    + " were detected, at most one container-image extension can be present. ");
        }
    }
}
