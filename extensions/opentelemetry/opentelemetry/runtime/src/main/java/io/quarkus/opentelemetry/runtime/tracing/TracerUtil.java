package io.quarkus.opentelemetry.runtime.tracing;

import java.util.List;
import java.util.Optional;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.opentelemetry.runtime.OpenTelemetryUtil;

public class TracerUtil {
    private TracerUtil() {
    }

    public static Resource mapResourceAttributes(List<String> resourceAttributes) {
        AttributesBuilder attributesBuilder = Attributes.builder();

        OpenTelemetryUtil.convertKeyValueListToMap(resourceAttributes).forEach(attributesBuilder::put);

        return Resource.create(attributesBuilder.build());
    }

    private static Sampler getBaseSampler(String samplerName, Optional<Double> ratio) {
        switch (samplerName) {
            case "on":
                return Sampler.alwaysOn();
            case "off":
                return Sampler.alwaysOff();
            case "ratio":
                return Sampler.traceIdRatioBased(ratio.orElse(1.0d));
            default:
                throw new IllegalArgumentException("Unrecognized value for sampler: " + samplerName);
        }
    }

    public static Sampler mapSampler(TracerRuntimeConfig.SamplerConfig samplerConfig, boolean suppressNonApplicationUris) {
        Sampler sampler = getBaseSampler(samplerConfig.samplerName, samplerConfig.ratio);

        if (suppressNonApplicationUris) {
            sampler = new NonApplicationEndpointSampler(sampler);
        }

        if (samplerConfig.parentBased) {
            return Sampler.parentBased(sampler);
        }

        return sampler;
    }
}
