package io.quarkus.smallrye.openapi.common.deployment;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

public class OpenApiConfigSourceInterceptor implements ConfigSourceInterceptor {
    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        ConfigValue configValue = context.proceed(name);

        if (!name.startsWith("quarkus.smallrye-openapi")) {
            return configValue;
        }

        if (configValue == null || configValue.getValue() == null
                || !configValue.getValue().equals("openapi-<document-name>")) {
            return configValue;
        }

        if (name.endsWith("path")) {
            if (!SmallRyeOpenApiConfig.DEFAULT_PATH.equals(configValue.getValue())) {
                return configValue;
            }
        } else if (name.endsWith("store-schema-file-name")) {
            if (!SmallRyeOpenApiConfig.DEFAULT_STORE_SCHEMA_FILE_NAME.equals(configValue.getValue())) {
                return configValue;
            }
        } else {
            return configValue;
        }

        String[] splits = name.split("\\.");

        if (splits.length == 3) {
            return configValue.from().withValue("openapi").build();
        } else if (splits.length == 4) {
            String documentName = splits[2];
            return configValue.from().withValue("openapi-" + documentName).build();
        }

        return configValue;
    }
}
