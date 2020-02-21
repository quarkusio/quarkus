package io.quarkus.container.image.deployment;

import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;

public class ContainerProcessor {

    @BuildStep
    public ContainerImageInfoBuildItem publishImageInfo(ApplicationInfoBuildItem app,
            ContainerImageConfig containerImageConfig) {
        return new ContainerImageInfoBuildItem(containerImageConfig.registry,
                containerImageConfig.group,
                containerImageConfig.name.orElse(app.getName()),
                containerImageConfig.tag.orElse(app.getVersion()));
    }
}
