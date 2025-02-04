package io.quarkus.container.image.podman.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.container.image.deployment.ContainerImageConfig;

public class PodmanBuild implements BooleanSupplier {
    private final ContainerImageConfig containerImageConfig;

    public PodmanBuild(ContainerImageConfig containerImageConfig) {
        this.containerImageConfig = containerImageConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return containerImageConfig.builder()
                .map(b -> b.equals(PodmanProcessor.PODMAN_CONTAINER_IMAGE_NAME))
                .orElse(true);
    }
}
