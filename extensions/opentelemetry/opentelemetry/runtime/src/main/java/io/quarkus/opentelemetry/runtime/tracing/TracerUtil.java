package io.quarkus.opentelemetry.runtime.tracing;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.samplers.Sampler;

public class TracerUtil {
    private TracerUtil() {
    }

    public static Resource mapResourceAttributes(List<String> resourceAttributes) {
        AttributesBuilder attributesBuilder = Attributes.builder();

        resourceAttributes.stream()
                .map(keyValuePair -> keyValuePair.split("=", 2))
                .map(keyValuePair -> new AbstractMap.SimpleImmutableEntry<>(keyValuePair[0].trim(), keyValuePair[1].trim()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, next) -> next, LinkedHashMap::new))
                .forEach(attributesBuilder::put);

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

    public static Sampler mapSampler(TracerRuntimeConfig.SamplerConfig samplerConfig) {
        Sampler sampler = getBaseSampler(samplerConfig.samplerName, samplerConfig.ratio);

        if (samplerConfig.parentBased) {
            return Sampler.parentBased(sampler);
        }

        return sampler;
    }
}
