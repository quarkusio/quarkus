package io.quarkus.opentelemetry.runtime.config.build;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * Will convert values from the legacy configuration <code>quarkus.opentelemetry.tracer.sampler.sampler.name</code>.
 */
public class LegacySamplerNameConverter implements Converter<String> {
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
