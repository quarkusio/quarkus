package io.quarkus.opentelemetry.runtime.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Priority;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Priorities;
import io.smallrye.config.RelocateConfigSourceInterceptor;

@Priority(Priorities.LIBRARY + 600 - 5)
public class OtelConfigRelocateConfigSourceInterceptor extends RelocateConfigSourceInterceptor {
    public static final Map<String, String> RELOCATIONS = relocations();

    public OtelConfigRelocateConfigSourceInterceptor() {
        super(RELOCATIONS);
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        final Set<String> names = new HashSet<>();
        final Iterator<String> namesIterator = context.iterateNames();
        while (namesIterator.hasNext()) {
            final String name = namesIterator.next();
            names.add(name);
            final String mappedName = RELOCATIONS.get(name);
            if (mappedName != null) {
                names.add(mappedName);
            }
        }
        return names.iterator();
    }

    @Override
    public Iterator<ConfigValue> iterateValues(final ConfigSourceInterceptorContext context) {
        return context.iterateValues();
    }

    private static Map<String, String> relocations() {
        Map<String, String> relocations = new HashMap<>();
        relocations.put("quarkus.opentelemetry.tracer.exporter.otlp.enabled", "quarkus.otel.exporter.otlp.enabled");
        relocations.put("quarkus.opentelemetry.tracer.exporter.otlp.endpoint",
                "quarkus.otel.exporter.otlp.traces.legacy-endpoint");
        relocations.put("quarkus.opentelemetry.enabled", "quarkus.otel.enabled");
        relocations.put("quarkus.opentelemetry.tracer.enabled", "quarkus.otel.traces.enabled");
        relocations.put("quarkus.opentelemetry.tracer.sampler", "quarkus.otel.traces.sampler");
        relocations.put("quarkus.opentelemetry.tracer.sampler.ratio", "quarkus.otel.traces.sampler.arg");
        relocations.put("quarkus.opentelemetry.tracer.suppress-non-application-uris",
                "quarkus.otel.traces.suppress-non-application-uris");
        relocations.put("quarkus.opentelemetry.tracer.include-static-resources",
                "quarkus.otel.traces.include-static-resources");
        relocations.put("quarkus.opentelemetry.tracer.exporter.otlp.headers",
                "quarkus.otel.exporter.otlp.traces.headers");
        return Collections.unmodifiableMap(relocations);
    }
}
