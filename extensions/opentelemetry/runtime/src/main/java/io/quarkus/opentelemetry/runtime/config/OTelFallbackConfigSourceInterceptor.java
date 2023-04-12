package io.quarkus.opentelemetry.runtime.config;

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
    private final static LegacySamplerNameConverter LEGACY_SAMPLER_NAME_CONVERTER = new LegacySamplerNameConverter();

    public OTelFallbackConfigSourceInterceptor() {
        super(Map.of(
                "quarkus.otel.enabled", "quarkus.opentelemetry.enabled",
                "quarkus.otel.traces.enabled", "quarkus.opentelemetry.tracer.enabled",
                "quarkus.otel.propagators", "quarkus.opentelemetry.propagators",
                "quarkus.otel.traces.suppress-non-application-uris",
                "quarkus.opentelemetry.tracer.suppress-non-application-uris",
                "quarkus.otel.traces.include-static-resources", "quarkus.opentelemetry.tracer.include-static-resources",
                "quarkus.otel.traces.sampler", "quarkus.opentelemetry.tracer.sampler",
                "quarkus.otel.traces.sampler.arg", "quarkus.opentelemetry.tracer.sampler.ratio",
                "quarkus.otel.exporter.otlp.enabled", "quarkus.opentelemetry.tracer.exporter.otlp.enabled",
                "quarkus.otel.exporter.otlp.traces.headers", "quarkus.opentelemetry.tracer.exporter.otlp.headers",
                "quarkus.otel.exporter.otlp.traces.legacy-endpoint", "quarkus.opentelemetry.tracer.exporter.otlp.endpoint"));
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        ConfigValue value = super.getValue(context, name);
        if (name.equals("quarkus.otel.traces.sampler")) {
            return value.withValue(LEGACY_SAMPLER_NAME_CONVERTER.convert(value.getValue()));
        }
        return value;
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        Set<String> names = new HashSet<>();
        Iterator<String> namesIterator = super.iterateNames(context);
        while (namesIterator.hasNext()) {
            names.add(namesIterator.next());
        }
        // TODO - Required because the defaults ConfigSource for mappings does not provide configuration names.
        names.add("quarkus.otel.enabled");
        names.add("quarkus.otel.metrics.exporter");
        names.add("quarkus.otel.propagators");
        names.add("quarkus.otel.logs.exporter");
        names.add("quarkus.otel.traces.enabled");
        names.add("quarkus.otel.traces.exporter");
        names.add("quarkus.otel.traces.sampler");
        names.add("quarkus.otel.sdk.disabled");
        names.add("quarkus.otel.service.name");
        names.add("quarkus.otel.attribute.value.length.limit");
        names.add("quarkus.otel.attribute.count.limit");
        names.add("quarkus.otel.span.attribute.count.limit");
        names.add("quarkus.otel.span.event.count.limit");
        names.add("quarkus.otel.span.link.count.limit");
        names.add("quarkus.otel.bsp.schedule.delay");
        names.add("quarkus.otel.bsp.max.queue.size");
        names.add("quarkus.otel.bsp.max.export.batch.size");
        names.add("quarkus.otel.bsp.export.timeout");
        names.add("quarkus.otel.experimental.resource.disabled-keys");
        return names.iterator();
    }
}
