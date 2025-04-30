
package io.quarkus.container.image.buildpack.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.container.image.deployment.ContainerImageConfig;

public class BuildpackBuildEnabled implements BooleanSupplier {

    private final ContainerImageConfig containerImageConfig;

    BuildpackBuildEnabled(ContainerImageConfig containerImageConfig) {
        this.containerImageConfig = containerImageConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return containerImageConfig.builder().map(b -> b.equals(BuildpackProcessor.BUILDPACK)).orElse(true);
    }
}
