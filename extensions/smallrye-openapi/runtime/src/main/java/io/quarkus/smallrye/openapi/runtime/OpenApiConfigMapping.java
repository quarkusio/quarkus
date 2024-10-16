package io.quarkus.smallrye.openapi.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.Converter;
import org.eclipse.microprofile.openapi.OASConfig;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Converters;
import io.smallrye.config.RelocateConfigSourceInterceptor;
import io.smallrye.openapi.api.OpenApiConfig.OperationIdStrategy;
import io.smallrye.openapi.api.SmallRyeOASConfig;

/**
 * Maps config from MicroProfile and SmallRye to Quarkus
 */
public class OpenApiConfigMapping extends RelocateConfigSourceInterceptor {
    private static final long serialVersionUID = 1L;
    private static final Map<String, String> RELOCATIONS = relocations();
    private static final Converter<OperationIdStrategy> OPERATION_ID_STRATEGY_CONVERTER = Converters
            .getImplicitConverter(OperationIdStrategy.class);

    public OpenApiConfigMapping() {
        super(RELOCATIONS);
    }

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        ConfigValue configValue = super.getValue(context, name);

        // Special case for enum. The converter run after the interceptors, so we have to do this here.
        if (configValue != null && name.equals(SmallRyeOASConfig.OPERATION_ID_STRAGEGY)) {
            String correctValue = OPERATION_ID_STRATEGY_CONVERTER.convert(configValue.getValue()).toString();
            configValue = configValue.withValue(correctValue);
        }

        return configValue;
    }

    private static Map<String, String> relocations() {
        Map<String, String> relocations = new HashMap<>();
        mapKey(relocations, SmallRyeOASConfig.VERSION, QUARKUS_OPEN_API_VERSION);
        mapKey(relocations, OASConfig.SERVERS, QUARKUS_SERVERS);
        mapKey(relocations, SmallRyeOASConfig.INFO_TITLE, QUARKUS_INFO_TITLE);
        mapKey(relocations, SmallRyeOASConfig.INFO_VERSION, QUARKUS_INFO_VERSION);
        mapKey(relocations, SmallRyeOASConfig.INFO_DESCRIPTION, QUARKUS_INFO_DESCRIPTION);
        mapKey(relocations, SmallRyeOASConfig.INFO_TERMS, QUARKUS_INFO_TERMS);
        mapKey(relocations, SmallRyeOASConfig.INFO_CONTACT_EMAIL, QUARKUS_INFO_CONTACT_EMAIL);
        mapKey(relocations, SmallRyeOASConfig.INFO_CONTACT_NAME, QUARKUS_INFO_CONTACT_NAME);
        mapKey(relocations, SmallRyeOASConfig.INFO_CONTACT_URL, QUARKUS_INFO_CONTACT_URL);
        mapKey(relocations, SmallRyeOASConfig.INFO_LICENSE_NAME, QUARKUS_INFO_LICENSE_NAME);
        mapKey(relocations, SmallRyeOASConfig.INFO_LICENSE_URL, QUARKUS_INFO_LICENSE_URL);
        mapKey(relocations, SmallRyeOASConfig.OPERATION_ID_STRAGEGY, QUARKUS_OPERATION_ID_STRATEGY);
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
    private static final String QUARKUS_OPERATION_ID_STRATEGY = "quarkus.smallrye-openapi.operation-id-strategy";

}
