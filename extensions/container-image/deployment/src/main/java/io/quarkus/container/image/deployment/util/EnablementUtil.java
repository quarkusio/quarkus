package io.quarkus.container.image.deployment.util;

import java.util.Optional;

import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;

public class EnablementUtil {

    public static boolean buildContainerImageNeeded(ContainerImageConfig containerImageConfig,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest) {
        return containerImageConfig.isBuildExplicitlyEnabled()
                || (buildRequest.isPresent() && !containerImageConfig.isBuildExplicitlyDisabled());
    }

    public static boolean pushContainerImageNeeded(ContainerImageConfig containerImageConfig,
            Optional<ContainerImagePushRequestBuildItem> pushRequest) {
        return containerImageConfig.isPushExplicitlyEnabled()
                // Never push an image when build is disabled (it may not even exist).
                || (pushRequest.isPresent() && !containerImageConfig.isBuildExplicitlyDisabled()
                        && !containerImageConfig.isPushExplicitlyDisabled());
    }

    public static boolean buildOrPushContainerImageNeeded(ContainerImageConfig containerImageConfig,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest) {
        return buildContainerImageNeeded(containerImageConfig, buildRequest)
                || pushContainerImageNeeded(containerImageConfig, pushRequest);
    }
}
