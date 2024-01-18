package io.quarkus.opentelemetry.runtime.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Priority;

import io.quarkus.opentelemetry.runtime.config.build.LegacySamplerNameConverter;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.FallbackConfigSourceInterceptor;
import io.smallrye.config.Priorities;

@Priority(Priorities.LIBRARY + 300 + 5)
public class OTelFallbackConfigSourceInterceptor extends FallbackConfigSourceInterceptor {
    private final static Map<String, String> FALLBACKS = new HashMap<>();
    private final static LegacySamplerNameConverter LEGACY_SAMPLER_NAME_CONVERTER = new LegacySamplerNameConverter();

    static {
        FALLBACKS.put("quarkus.otel.enabled", "quarkus.opentelemetry.enabled");
        FALLBACKS.put("quarkus.otel.traces.enabled", "quarkus.opentelemetry.tracer.enabled");
        FALLBACKS.put("quarkus.otel.propagators", "quarkus.opentelemetry.propagators");
        FALLBACKS.put("quarkus.otel.resource.attributes", "quarkus.opentelemetry.tracer.resource-attributes");
        FALLBACKS.put("quarkus.otel.traces.suppress-non-application-uris",
                "quarkus.opentelemetry.tracer.suppress-non-application-uris");
        FALLBACKS.put("quarkus.otel.traces.include-static-resources", "quarkus.opentelemetry.tracer.include-static-resources");
        FALLBACKS.put("quarkus.otel.traces.sampler", "quarkus.opentelemetry.tracer.sampler");
        FALLBACKS.put("quarkus.otel.traces.sampler.arg", "quarkus.opentelemetry.tracer.sampler.ratio");
        FALLBACKS.put("quarkus.otel.exporter.otlp.enabled", "quarkus.opentelemetry.tracer.exporter.otlp.enabled");
        FALLBACKS.put("quarkus.otel.exporter.otlp.traces.legacy-endpoint",
                "quarkus.opentelemetry.tracer.exporter.otlp.endpoint");
        FALLBACKS.put("quarkus.otel.exporter.otlp.traces.headers", "quarkus.opentelemetry.tracer.exporter.otlp.headers");
    }

    public OTelFallbackConfigSourceInterceptor() {
        super(FALLBACKS);
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        ConfigValue value = super.getValue(context, name);
        if (value != null && name.equals("quarkus.otel.traces.sampler")) {
            return value.withValue(LEGACY_SAMPLER_NAME_CONVERTER.convert(value.getValue()));
        }
        return value;
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        Set<String> names = new HashSet<>();
        Iterator<String> namesIterator = context.iterateNames();
        while (namesIterator.hasNext()) {
            String name = namesIterator.next();
            String fallback = FALLBACKS.get(name);
            // We only include the used property, so if it is a fallback (not mapped), it will be reported as unknown
            if (fallback != null) {
                ConfigValue nameValue = context.proceed(name);
                ConfigValue fallbackValue = context.proceed(fallback);
                if (nameValue == null) {
                    names.add(fallback);
                } else if (fallbackValue == null) {
                    names.add(name);
                } else if (nameValue.getConfigSourceOrdinal() >= fallbackValue.getConfigSourceOrdinal()) {
                    names.add(name);
                } else {
                    names.add(fallback);
                }
            } else {
                names.add(name);
            }
        }
        return names.iterator();
    }
}
