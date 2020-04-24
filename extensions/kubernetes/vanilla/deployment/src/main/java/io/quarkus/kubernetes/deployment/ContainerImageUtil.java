package io.quarkus.kubernetes.deployment;

import static io.quarkus.container.image.deployment.util.ImageUtil.hasRegistry;

import java.util.Optional;

import io.quarkus.container.image.deployment.ContainerImageCapabilitiesUtil;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.Capabilities;

final class ContainerImageUtil {

    private ContainerImageUtil() {
    }

    static boolean isRegistryMissingAndNotS2I(Capabilities capabilities, ContainerImageInfoBuildItem containerImageInfo) {
        Optional<String> activeContainerImageCapability = ContainerImageCapabilitiesUtil
                .getActiveContainerImageCapability(capabilities);
        if (!activeContainerImageCapability.isPresent()) { // shouldn't ever happen when this method is called
            return false;
        }

        return !hasRegistry(containerImageInfo.getImage())
                && !Capabilities.CONTAINER_IMAGE_S2I.equals(activeContainerImageCapability.get());
    }
}
