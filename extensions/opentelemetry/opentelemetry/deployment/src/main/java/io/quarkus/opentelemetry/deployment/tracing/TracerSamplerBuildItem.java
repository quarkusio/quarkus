package io.quarkus.opentelemetry.deployment.tracing;

import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.opentelemetry.runtime.tracing.LateBoundSampler;
import io.quarkus.runtime.RuntimeValue;

public final class TracerSamplerBuildItem extends SimpleBuildItem {
    private final RuntimeValue<Optional<LateBoundSampler>> sampler;

    public TracerSamplerBuildItem(RuntimeValue<Optional<LateBoundSampler>> sampler) {
        this.sampler = sampler;
    }

    public RuntimeValue<Optional<LateBoundSampler>> getSampler() {
        return sampler;
    }
}
