package io.quarkus.smallrye.graphql.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.smallrye.config.ConfigSourceInterceptorContext;
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

    private static Map<String, String> relocations() {
        Map<String, String> relocations = new HashMap<>();
        mapKey(relocations, ConfigKey.ALLOW_GET, QUARKUS_HTTP_GET_ENABLED);
        mapKey(relocations, ConfigKey.ALLOW_POST_WITH_QUERY_PARAMETERS, QUARKUS_HTTP_POST_QUERYPARAMETERS_ENABLED);
        mapKey(relocations, ConfigKey.ERROR_EXTENSION_FIELDS, QUARKUS_ERROR_EXTENSION_FIELDS);
        mapKey(relocations, ConfigKey.DEFAULT_ERROR_MESSAGE, QUARKUS_DEFAULT_ERROR_MESSAGE);
        mapKey(relocations, ConfigKey.PRINT_DATAFETCHER_EXCEPTION, QUARKUS_PRINT_DATAFETCHER_EXCEPTION);
        mapKey(relocations, ConfigKey.SCHEMA_INCLUDE_SCALARS, QUARKUS_SCHEMA_INCLUDE_SCALARS);
        mapKey(relocations, ConfigKey.SCHEMA_INCLUDE_DEFINITION, QUARKUS_SCHEMA_INCLUDE_DEFINITION);
        mapKey(relocations, ConfigKey.SCHEMA_INCLUDE_DIRECTIVES, QUARKUS_SCHEMA_INCLUDE_DIRECTIVES);
        mapKey(relocations, ConfigKey.SCHEMA_INCLUDE_INTROSPECTION_TYPES, QUARKUS_SCHEMA_INCLUDE_INTROSPECTION_TYPES);
        mapKey(relocations, ConfigKey.LOG_PAYLOAD, QUARKUS_LOG_PAYLOAD);
        mapKey(relocations, ConfigKey.FIELD_VISIBILITY, QUARKUS_FIELD_VISIBILITY);
        mapKey(relocations, ConfigKey.UNWRAP_EXCEPTIONS, QUARKUS_UNWRAP_EXCEPTIONS);
        mapKey(relocations, SHOW_ERROR_MESSAGE, QUARKUS_SHOW_ERROR_MESSAGE);
        mapKey(relocations, HIDE_ERROR_MESSAGE, QUARKUS_HIDE_ERROR_MESSAGE);
        return Collections.unmodifiableMap(relocations);
    }

    private static void mapKey(Map<String, String> map, String quarkusKey, String otherKey) {
        map.put(quarkusKey, otherKey);
        map.put(otherKey, quarkusKey);
    }

    private static final String SHOW_ERROR_MESSAGE = "mp.graphql.showErrorMessage";
    private static final String HIDE_ERROR_MESSAGE = "mp.graphql.hideErrorMessage";
    private static final String QUARKUS_ERROR_EXTENSION_FIELDS = "quarkus.smallrye-graphql.error-extension-fields";
    private static final String QUARKUS_DEFAULT_ERROR_MESSAGE = "quarkus.smallrye-graphql.default-error-message";
    private static final String QUARKUS_SHOW_ERROR_MESSAGE = "quarkus.smallrye-graphql.show-runtime-exception-message";
    private static final String QUARKUS_HIDE_ERROR_MESSAGE = "quarkus.smallrye-graphql.hide-checked-exception-message";
    private static final String QUARKUS_HTTP_GET_ENABLED = "quarkus.smallrye-graphql.http.get.enabled";
    private static final String QUARKUS_HTTP_POST_QUERYPARAMETERS_ENABLED = "quarkus.smallrye-graphql.http.post.queryparameters.enabled";
    private static final String QUARKUS_PRINT_DATAFETCHER_EXCEPTION = "quarkus.smallrye-graphql.print-data-fetcher-exception";
    private static final String QUARKUS_SCHEMA_INCLUDE_SCALARS = "quarkus.smallrye-graphql.schema-include-scalars";
    private static final String QUARKUS_SCHEMA_INCLUDE_DEFINITION = "quarkus.smallrye-graphql.schema-include-schema-definition";
    private static final String QUARKUS_SCHEMA_INCLUDE_DIRECTIVES = "quarkus.smallrye-graphql.schema-include-directives";
    private static final String QUARKUS_SCHEMA_INCLUDE_INTROSPECTION_TYPES = "quarkus.smallrye-graphql.schema-include-introspection-types";
    private static final String QUARKUS_LOG_PAYLOAD = "quarkus.smallrye-graphql.log-payload";
    private static final String QUARKUS_FIELD_VISIBILITY = "quarkus.smallrye-graphql.field-visibility";
    private static final String QUARKUS_UNWRAP_EXCEPTIONS = "quarkus.smallrye-graphql.unwrap-exceptions";

}
