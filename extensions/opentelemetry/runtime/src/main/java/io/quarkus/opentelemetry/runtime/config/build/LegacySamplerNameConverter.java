package io.quarkus.opentelemetry.runtime.config.build;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * Will convert values from the legacy configuration under
 * opentelemetry.tracer.sampler.sampler.name
 * <br>
 * This was used by the {@link io.quarkus.opentelemetry.runtime.tracing.LateBoundSampler}
 */
public class LegacySamplerNameConverter implements Converter<String> {

    public LegacySamplerNameConverter() {
    }

    @Override
    public String convert(String samplerName) throws IllegalArgumentException, NullPointerException {
        switch (samplerName) {
            case "on":
                return SamplerType.Constants.ALWAYS_ON;
            case "off":
                return SamplerType.Constants.ALWAYS_OFF;
            case "ratio":
                return SamplerType.Constants.TRACE_ID_RATIO;
            default:
                return samplerName;
        }
    }
}
