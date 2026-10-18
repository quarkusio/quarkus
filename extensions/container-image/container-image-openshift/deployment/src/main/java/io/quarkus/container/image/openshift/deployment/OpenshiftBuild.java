package io.quarkus.container.image.openshift.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.container.image.deployment.ContainerImageConfig;

public class OpenshiftBuild implements BooleanSupplier {

    private final ContainerImageConfig containerImageConfig;

    OpenshiftBuild(ContainerImageConfig containerImageConfig) {
        this.containerImageConfig = containerImageConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return containerImageConfig.builder().map(b -> b.equals(OpenshiftProcessor.OPENSHIFT)).orElse(true);
    }
}
