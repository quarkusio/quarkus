package io.quarkus.smallrye.openapi.deployment;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.quarkus.runtime.configuration.HyphenateEnumConverter;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.RelocateConfigSourceInterceptor;
import io.smallrye.openapi.api.OpenApiConfig;

/**
 * Maps config from MicroProfile and SmallRye to Quarkus
 */
public class OpenApiConfigMapping extends RelocateConfigSourceInterceptor {
    private static final Map<String, String> RELOCATIONS = relocations();
    private final HyphenateEnumConverter enumConverter = new HyphenateEnumConverter(OpenApiConfig.OperationIdStrategy.class);

    public OpenApiConfigMapping() {
        super(RELOCATIONS);
    }

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        ConfigValue configValue = super.getValue(context, name);
        // Special case for enum. The converter run after the interceptors, so we have to do this here.
        if (name.equals(io.smallrye.openapi.api.constants.OpenApiConstants.OPERATION_ID_STRAGEGY)) {
            if (configValue != null) {
                String correctValue = enumConverter.convert(configValue.getValue()).toString();
                configValue = configValue.withValue(correctValue);
            }
        }
        return configValue;
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
        mapKey(relocations, io.smallrye.openapi.api.constants.OpenApiConstants.OPEN_API_VERSION, QUARKUS_OPEN_API_VERSION);
        mapKey(relocations, org.eclipse.microprofile.openapi.OASConfig.SERVERS, QUARKUS_SERVERS);
        mapKey(relocations, io.smallrye.openapi.api.constants.OpenApiConstants.INFO_TITLE, QUARKUS_INFO_TITLE);
        mapKey(relocations, io.smallrye.openapi.api.constants.OpenApiConstants.INFO_VERSION, QUARKUS_INFO_VERSION);
        mapKey(relocations, io.smallrye.openapi.api.constants.OpenApiConstants.INFO_DESCRIPTION, QUARKUS_INFO_DESCRIPTION);
        mapKey(relocations, io.smallrye.openapi.api.constants.OpenApiConstants.INFO_TERMS, QUARKUS_INFO_TERMS);
        mapKey(relocations, io.smallrye.openapi.api.constants.OpenApiConstants.INFO_CONTACT_EMAIL, QUARKUS_INFO_CONTACT_EMAIL);
        mapKey(relocations, io.smallrye.openapi.api.constants.OpenApiConstants.INFO_CONTACT_NAME, QUARKUS_INFO_CONTACT_NAME);
        mapKey(relocations, io.smallrye.openapi.api.constants.OpenApiConstants.INFO_CONTACT_URL, QUARKUS_INFO_CONTACT_URL);
        mapKey(relocations, io.smallrye.openapi.api.constants.OpenApiConstants.INFO_LICENSE_NAME, QUARKUS_INFO_LICENSE_NAME);
        mapKey(relocations, io.smallrye.openapi.api.constants.OpenApiConstants.INFO_LICENSE_URL, QUARKUS_INFO_LICENSE_URL);
        mapKey(relocations, io.smallrye.openapi.api.constants.OpenApiConstants.OPERATION_ID_STRAGEGY,
                QUARKUS_OPERATION_ID_STRAGEGY);
        return Collections.unmodifiableMap(relocations);
    }

    private static void mapKey(Map<String, String> map, String quarkusKey, String otherKey) {
        map.put(quarkusKey, otherKey);
        map.put(otherKey, quarkusKey);
    }

    private static final String QUARKUS_OPEN_API_VERSION = "quarkus.smallrye-openapi.open-api-version";
    private static final String QUARKUS_SERVERS = "quarkus.smallrye-openapi.servers";
    private static final String QUARKUS_INFO_TITLE = "quarkus.smallrye-openapi.info-title";
    private static final String QUARKUS_INFO_VERSION = "quarkus.smallrye-openapi.info-version";
    private static final String QUARKUS_INFO_DESCRIPTION = "quarkus.smallrye-openapi.info-description";
    private static final String QUARKUS_INFO_TERMS = "quarkus.smallrye-openapi.info-terms-of-service";
    private static final String QUARKUS_INFO_CONTACT_EMAIL = "quarkus.smallrye-openapi.info-contact-email";
    private static final String QUARKUS_INFO_CONTACT_NAME = "quarkus.smallrye-openapi.info-contact-name";
    private static final String QUARKUS_INFO_CONTACT_URL = "quarkus.smallrye-openapi.info-contact-url";
    private static final String QUARKUS_INFO_LICENSE_NAME = "quarkus.smallrye-openapi.info-license-name";
    private static final String QUARKUS_INFO_LICENSE_URL = "quarkus.smallrye-openapi.info-license-url";
    private static final String QUARKUS_OPERATION_ID_STRAGEGY = "quarkus.smallrye-openapi.operation-id-strategy";

}
