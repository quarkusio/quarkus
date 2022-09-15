package io.quarkus.smallrye.opentracing.runtime;

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
public class TracingConfigRelocateConfigSourceInterceptor extends RelocateConfigSourceInterceptor {
    private static final Map<String, String> RELOCATIONS = relocations();

    public TracingConfigRelocateConfigSourceInterceptor() {
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
        relocations.put("mp.opentracing.server.skip-pattern", "quarkus.opentracing.server.skip-pattern");
        relocations.put("quarkus.opentracing.server.skip-pattern", "mp.opentracing.server.skip-pattern");
        relocations.put("mp.opentracing.server.operation-name-provider", "quarkus.opentracing.server.operation-name-provider");
        relocations.put("quarkus.opentracing.server.operation-name-provider", "mp.opentracing.server.operation-name-provider");
        return Collections.unmodifiableMap(relocations);
    }
}
