package io.quarkus.opentelemetry.runtime.tracing;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.opentelemetry.runtime.OpenTelemetryUtil;
import io.quarkus.opentelemetry.runtime.tracing.config.TracesBuildConfig;

public class TracerUtil {
    private TracerUtil() {
    }

    public static Resource mapResourceAttributes(List<String> resourceAttributes) {
        AttributesBuilder attributesBuilder = Attributes.builder();

        OpenTelemetryUtil.convertKeyValueListToMap(resourceAttributes).forEach(attributesBuilder::put);

        return Resource.create(attributesBuilder.build());
    }

    public static Resource mapResourceAttributes(Map<String, String> resourceAttributes) {
        AttributesBuilder attributesBuilder = Attributes.builder();

        resourceAttributes.forEach(attributesBuilder::put);

        return Resource.create(attributesBuilder.build());
    }

    private static Sampler getBaseSampler(String samplerName, Optional<Double> ratio) {
        switch (samplerName) {
            case "on":
            case "always_on":
            case "parentbased_always_on":
                return Sampler.alwaysOn();
            case "off":
            case "always_off":
            case "parentbased_always_off":
                return Sampler.alwaysOff();
            case "ratio":
            case "traceidratio":
            case "parentbased_traceidratio":
                return Sampler.traceIdRatioBased(ratio.orElse(1.0d));
            default:
                throw new IllegalArgumentException("Unrecognized value for sampler: " + samplerName);
        }
    }

    private static boolean getParentSampler(TracesBuildConfig.SamplerConfig samplerConfig) {
        switch (samplerConfig.sampler()) {
            case "parentbased_always_on":
            case "parentbased_always_off":
            case "parentbased_traceidratio":
                return true;
            default:
                return false;
        }
    }

    public static Sampler mapSampler(TracesBuildConfig.SamplerConfig samplerConfig, List<String> dropNames) {
        Sampler sampler = getBaseSampler(samplerConfig.sampler(), samplerConfig.arg());

        if (!dropNames.isEmpty()) {
            sampler = new DropTargetsSampler(sampler, dropNames);
        }

        if (getParentSampler(samplerConfig)) {
            return Sampler.parentBased(sampler);
        }

        return sampler;
    }
}
