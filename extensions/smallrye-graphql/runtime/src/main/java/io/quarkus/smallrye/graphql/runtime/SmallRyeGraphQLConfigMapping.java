package io.quarkus.smallrye.graphql.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.RelocateConfigSourceInterceptor;
import io.smallrye.graphql.cdi.config.ConfigKey;

/**
 * Maps config from MicroProfile and SmallRye to Quarkus
 */
public class SmallRyeGraphQLConfigMapping extends RelocateConfigSourceInterceptor {
    private static final Map<String, String> RELOCATIONS = relocations();

    public SmallRyeGraphQLConfigMapping() {
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
        mapKey(relocations, ConfigKey.ERROR_EXTENSION_FIELDS, "quarkus.smallrye-graphql.error-extension-fields");
        mapKey(relocations, ConfigKey.DEFAULT_ERROR_MESSAGE, "quarkus.smallrye-graphql.default-error-message");
        mapKey(relocations, "mp.graphql.showErrorMessage", "quarkus.smallrye-graphql.show-runtime-exception-message");
        mapKey(relocations, "mp.graphql.hideErrorMessage", "quarkus.smallrye-graphql.hide-checked-exception-message");
        return Collections.unmodifiableMap(relocations);
    }

    private static void mapKey(Map<String, String> map, String quarkusKey, String otherKey) {
        map.put(quarkusKey, otherKey);
        map.put(otherKey, quarkusKey);
    }

}
