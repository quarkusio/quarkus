
package io.quarkus.container.image.docker.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.container.image.deployment.ContainerImageConfig;

public class DockerBuild implements BooleanSupplier {

    private final ContainerImageConfig containerImageConfig;

    DockerBuild(ContainerImageConfig containerImageConfig) {
        this.containerImageConfig = containerImageConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return containerImageConfig.builder().map(b -> b.equals(DockerProcessor.DOCKER_CONTAINER_IMAGE_NAME)).orElse(true);
    }
}
