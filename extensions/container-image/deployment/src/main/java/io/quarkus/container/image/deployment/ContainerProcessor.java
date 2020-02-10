package io.quarkus.container.image.deployment;

import io.quarkus.container.image.deployment.util.ImageUtil;
import io.quarkus.container.spi.ContainerImageBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;

public class ContainerProcessor {

    @BuildStep
    public ContainerImageBuildItem publishNativeImageInfo(ApplicationInfoBuildItem app,
            ContainerImageConfig containerImageConfig) {
        String image = ImageUtil.getImage(containerImageConfig.registry, containerImageConfig.group,
                containerImageConfig.name.orElse(app.getName()), containerImageConfig.tag.orElse(app.getVersion()));
        return new ContainerImageBuildItem(image);
    }

}
