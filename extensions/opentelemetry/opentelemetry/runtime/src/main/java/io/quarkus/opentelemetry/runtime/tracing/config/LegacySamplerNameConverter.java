package io.quarkus.opentelemetry.runtime.tracing.config;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * Will convert values from the legacy configuration under
 * opentelemetry.tracer.sampler.sampler.name
 * <br>
 * This was used by the {@link io.quarkus.opentelemetry.runtime.tracing.LateBoundSampler}
 */
public class LegacySamplerNameConverter implements Converter<String> {
    @Override
    public String convert(String samplerName) throws IllegalArgumentException, NullPointerException {
        switch (samplerName) {
            case "on":
                return TracesBuildConfig.SamplerType.Constants.ALWAYS_ON;
            case "off":
                return TracesBuildConfig.SamplerType.Constants.ALWAYS_OFF;
            case "ratio":
                return TracesBuildConfig.SamplerType.Constants.TRACE_ID_RATIO;
            default:
                return samplerName;
        }
    }
}
