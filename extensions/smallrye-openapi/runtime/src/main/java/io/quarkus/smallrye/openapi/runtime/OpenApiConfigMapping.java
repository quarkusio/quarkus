package io.quarkus.smallrye.openapi.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.eclipse.microprofile.openapi.OASConfig;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.RelocateConfigSourceInterceptor;
import io.smallrye.config.common.utils.StringUtil;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.SmallRyeOASConfig;

/**
 * Maps config from MicroProfile and SmallRye to Quarkus
 */
public class OpenApiConfigMapping extends RelocateConfigSourceInterceptor {
    private static final long serialVersionUID = 1L;

    public OpenApiConfigMapping() {
        super(new Relocator());
        ((Relocator) getMapping()).setRelocations(Map.of());
    }

    public OpenApiConfigMapping(String documentName) {
        super(new Relocator());

        Map<String, String> relocations = relocations(documentName);
        ((Relocator) getMapping()).setRelocations(relocations);
    }

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        ConfigValue configValue = super.getValue(context, name);

        /*
         * Special case for operationId strategy that supports both enumerated values or a FQCN.
         * The converter run after the interceptors, so we have to do this here.
         */
        if (configValue != null && name.equals(SmallRyeOASConfig.OPERATION_ID_STRAGEGY)) {
            String correctValue = convertOperationIdStrategy(configValue.getValue());
            configValue = configValue.withValue(correctValue);
        }

        return configValue;
    }

    private String convertOperationIdStrategy(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        final String trimmedValue = value.trim();

        if (trimmedValue.isEmpty()) {
            return null;
        }

        return switch (StringUtil.skewer(trimmedValue)) {
            case "method" -> OpenApiConfig.OperationIdStrategy.METHOD;
            case "class-method" -> OpenApiConfig.OperationIdStrategy.CLASS_METHOD;
            case "package-class-method" -> OpenApiConfig.OperationIdStrategy.PACKAGE_CLASS_METHOD;
            default -> trimmedValue;
        };
    }

    private Map<String, String> relocations(String documentName) {
        Map<String, String> relocations = new HashMap<>();
        mapKey(relocations, SmallRyeOASConfig.VERSION, propertyName(documentName, QUARKUS_OPEN_API_VERSION_SUFFIX));
        mapKey(relocations, OASConfig.SERVERS, propertyName(documentName, QUARKUS_SERVERS_SUFFIX));
        mapKey(relocations, SmallRyeOASConfig.INFO_TITLE, propertyName(documentName, QUARKUS_INFO_TITLE_SUFFIX));
        mapKey(relocations, SmallRyeOASConfig.INFO_VERSION, propertyName(documentName, QUARKUS_INFO_VERSION_SUFFIX));
        mapKey(relocations, SmallRyeOASConfig.INFO_DESCRIPTION, propertyName(documentName, QUARKUS_INFO_DESCRIPTION_SUFFIX));
        mapKey(relocations, SmallRyeOASConfig.INFO_TERMS, propertyName(documentName, QUARKUS_INFO_TERMS_SUFFIX));
        mapKey(relocations, SmallRyeOASConfig.INFO_CONTACT_EMAIL,
                propertyName(documentName, QUARKUS_INFO_CONTACT_EMAIL_SUFFIX));
        mapKey(relocations, SmallRyeOASConfig.INFO_CONTACT_NAME, propertyName(documentName, QUARKUS_INFO_CONTACT_NAME_SUFFIX));
        mapKey(relocations, SmallRyeOASConfig.INFO_CONTACT_URL, propertyName(documentName, QUARKUS_INFO_CONTACT_URL_SUFFIX));
        mapKey(relocations, SmallRyeOASConfig.INFO_LICENSE_NAME, propertyName(documentName, QUARKUS_INFO_LICENSE_NAME_SUFFIX));
        mapKey(relocations, SmallRyeOASConfig.INFO_LICENSE_URL, propertyName(documentName, QUARKUS_INFO_LICENSE_URL_SUFFIX));
        mapKey(relocations, SmallRyeOASConfig.OPERATION_ID_STRAGEGY,
                propertyName(documentName, QUARKUS_OPERATION_ID_STRATEGY_SUFFIX));
        mapKey(relocations, SmallRyeOASConfig.SMALLRYE_MERGE_SCHEMA_EXAMPLES,
                propertyName(documentName, QUARKUS_MERGE_SCHEMA_EXAMPLES_SUFFIX));
        mapKey(relocations, SmallRyeOASConfig.SCAN_PROFILES,
                propertyName(documentName, QUARKUS_SCAN_PROFILES_SUFFIX));
        mapKey(relocations, SmallRyeOASConfig.SCAN_EXCLUDE_PROFILES,
                propertyName(documentName, QUARKUS_SCAN_EXCLUDE_PROFILES_SUFFIX));
        mapKey(relocations, propertyName(OpenApiConstants.DEFAULT_DOCUMENT_NAME, QUARKUS_ALWAYS_RUN_FILTER_SUFFIX),
                propertyName(documentName, QUARKUS_ALWAYS_RUN_FILTER_SUFFIX));
        return Collections.unmodifiableMap(relocations);
    }

    private static void mapKey(Map<String, String> map, String quarkusKey, String otherKey) {
        map.put(quarkusKey, otherKey);
        map.put(otherKey, quarkusKey);
    }

    private static final String QUARKUS_OPEN_API_VERSION_SUFFIX = "open-api-version";
    private static final String QUARKUS_SERVERS_SUFFIX = "servers";
    private static final String QUARKUS_INFO_TITLE_SUFFIX = "info-title";
    private static final String QUARKUS_INFO_VERSION_SUFFIX = "info-version";
    private static final String QUARKUS_INFO_DESCRIPTION_SUFFIX = "info-description";
    private static final String QUARKUS_INFO_TERMS_SUFFIX = "info-terms-of-service";
    private static final String QUARKUS_INFO_CONTACT_EMAIL_SUFFIX = "info-contact-email";
    private static final String QUARKUS_INFO_CONTACT_NAME_SUFFIX = "info-contact-name";
    private static final String QUARKUS_INFO_CONTACT_URL_SUFFIX = "info-contact-url";
    private static final String QUARKUS_INFO_LICENSE_NAME_SUFFIX = "info-license-name";
    private static final String QUARKUS_INFO_LICENSE_URL_SUFFIX = "info-license-url";
    private static final String QUARKUS_OPERATION_ID_STRATEGY_SUFFIX = "operation-id-strategy";
    private static final String QUARKUS_MERGE_SCHEMA_EXAMPLES_SUFFIX = "merge-schema-examples";
    private static final String QUARKUS_SCAN_PROFILES_SUFFIX = "scan-profiles";
    private static final String QUARKUS_SCAN_EXCLUDE_PROFILES_SUFFIX = "scan-exclude-profiles";
    private static final String QUARKUS_ALWAYS_RUN_FILTER_SUFFIX = "always-run-filter";

    private String propertyName(String documentName, String suffix) {
        String quarkusPrefix = "quarkus.smallrye-openapi";

        if (OpenApiConstants.DEFAULT_DOCUMENT_NAME.equals(documentName)) {
            return quarkusPrefix + "." + suffix;
        }

        return quarkusPrefix + "." + documentName + "." + suffix;
    }

    private static class Relocator implements UnaryOperator<String> {
        private Map<String, String> relocations;

        public void setRelocations(Map<String, String> relocations) {
            this.relocations = relocations;
        }

        @Override
        public String apply(String name) {
            return relocations.getOrDefault(name, name);
        }
    }
}
