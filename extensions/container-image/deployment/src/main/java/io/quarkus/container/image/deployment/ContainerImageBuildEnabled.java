package io.quarkus.container.image.deployment;

import java.util.function.BooleanSupplier;

public class ContainerImageBuildEnabled implements BooleanSupplier {

    private final ContainerImageConfig containerImageConfig;

    public ContainerImageBuildEnabled(ContainerImageConfig containerImageConfig) {
        this.containerImageConfig = containerImageConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return containerImageConfig.build;
    }
}
