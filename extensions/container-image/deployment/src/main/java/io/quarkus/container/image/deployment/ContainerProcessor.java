package io.quarkus.container.image.deployment;

import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;

public class ContainerProcessor {

    @BuildStep
    public ContainerImageInfoBuildItem publishImageInfo(ApplicationInfoBuildItem app,
            ContainerImageConfig containerImageConfig, Capabilities capabilities) {

        ensureSingleContainerImageExtension(capabilities);

        return new ContainerImageInfoBuildItem(containerImageConfig.registry,
                containerImageConfig.getEffectiveGroup(),
                containerImageConfig.name.orElse(app.getName()),
                containerImageConfig.tag.orElse(app.getVersion()));
    }

    private void ensureSingleContainerImageExtension(Capabilities capabilities) {
        ContainerImageCapabilitiesUtil.getActiveContainerImageCapability(capabilities);
    }
}
