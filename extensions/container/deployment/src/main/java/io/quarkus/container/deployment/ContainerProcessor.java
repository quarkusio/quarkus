package io.quarkus.container.deployment;

import io.quarkus.container.deployment.util.ImageUtil;
import io.quarkus.container.spi.ContainerImageBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;

public class ContainerProcessor {

    private ContainerConfig containerConfig;

    @BuildStep(onlyIfNot = NativeBuild.class)
    public ContainerImageBuildItem publishImageInfo(ApplicationInfoBuildItem app) {
        String image = ImageUtil.getImage(containerConfig.registry, containerConfig.group,
                containerConfig.name.orElse(app.getName()), containerConfig.tag.orElse(app.getVersion()));
        return new ContainerImageBuildItem(image);
    }

    @BuildStep(onlyIf = NativeBuild.class)
    public ContainerImageBuildItem publishNativeImageInfo(ApplicationInfoBuildItem app) {
        String image = ImageUtil.getImage(containerConfig.registry, containerConfig.group,
                containerConfig.name.orElse(app.getName()), containerConfig.tag.orElse(app.getVersion() + "-native"));
        return new ContainerImageBuildItem(image);
    }

    public ContainerConfig getContainerConfig() {
        return this.containerConfig;
    }

    public void setContainerConfig(ContainerConfig containerConfig) {
        this.containerConfig = containerConfig;
    }

}
