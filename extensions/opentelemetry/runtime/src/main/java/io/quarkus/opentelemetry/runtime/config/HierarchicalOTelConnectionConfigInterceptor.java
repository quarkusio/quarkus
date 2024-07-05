package io.quarkus.opentelemetry.runtime.config;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import jakarta.annotation.Priority;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.FallbackConfigSourceInterceptor;
import io.smallrye.config.Priorities;

@Priority(Priorities.LIBRARY + 300 + 5)
public class HierarchicalOTelConnectionConfigInterceptor extends FallbackConfigSourceInterceptor {

    // The properties that are shared between the traces and metrics configuration
    // string after "quarkus.otel.exporter.otlp."
    private final static List<String> PROPERTY_NAMES = List.of(
            "endpoint",
            "headers",
            "compression",
            "timeout",
            "protocol",
            "key-cert.keys",
            "key-cert.certs",
            "trust-cert.certs",
            "tls-configuration-name",
            "proxy-options.enabled",
            "proxy-options.username",
            "proxy-options.password",
            "proxy-options.port",
            "proxy-options.host");

    static final String BASE = "quarkus.otel.exporter.otlp.";
    static final String TRACES = BASE + "traces.";
    static final String METRICS = BASE + "metrics.";

    private static final MappingFunction mappingFunction = new MappingFunction();

    public HierarchicalOTelConnectionConfigInterceptor() {
        super(mappingFunction);
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        Set<String> names = new HashSet<>();
        Iterator<String> namesIterator = context.iterateNames();
        while (namesIterator.hasNext()) {
            String name = namesIterator.next();
            String fallback = mappingFunction.apply(name);
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

    static class MappingFunction implements Function<String, String> {
        @Override
        public String apply(String name) {
            if (name.startsWith(TRACES)) {
                String property = name.substring(TRACES.length());
                if (PROPERTY_NAMES.contains(property)) {
                    return BASE + property;
                }
            }
            if (name.startsWith(METRICS)) {
                String property = name.substring(METRICS.length());
                if (PROPERTY_NAMES.contains(property)) {
                    return BASE + property;
                }
            }
            return name;
        }
    }
}
