package io.quarkus.container.image.s2i.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.container.image.deployment.ContainerImageConfig;

public class S2iBuild implements BooleanSupplier {

    private ContainerImageConfig containerImageConfig;

    S2iBuild(ContainerImageConfig containerImageConfig) {
        this.containerImageConfig = containerImageConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return containerImageConfig.builder.map(b -> b.equals(S2iProcessor.S2I)).orElse(true);
    }
}
