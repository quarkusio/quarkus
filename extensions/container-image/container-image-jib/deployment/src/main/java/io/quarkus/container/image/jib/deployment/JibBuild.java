package io.quarkus.container.image.jib.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.container.image.deployment.ContainerImageConfig;

public class JibBuild implements BooleanSupplier {

    private final ContainerImageConfig containerImageConfig;

    public JibBuild(ContainerImageConfig containerImageConfig) {
        this.containerImageConfig = containerImageConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return containerImageConfig.builder().map(b -> b.equals(JibProcessor.JIB)).orElse(true);
    }
}
