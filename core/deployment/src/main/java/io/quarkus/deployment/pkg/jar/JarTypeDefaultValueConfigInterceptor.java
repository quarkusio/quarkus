package io.quarkus.deployment.pkg.jar;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

/**
 * Adjust the default value of quarkus.package.jar.type if AOT is enabled.
 */
public class JarTypeDefaultValueConfigInterceptor implements ConfigSourceInterceptor {

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        ConfigValue configValue = context.proceed(name);

        if (!"quarkus.package.jar.type".equals(name)) {
            return configValue;
        }
        if (configValue != null && !configValue.isDefault()) {
            return configValue;
        }

        ConfigValue aotEnabled = context.proceed("quarkus.package.jar.aot.enabled");
        boolean isAot = aotEnabled != null && "true".equalsIgnoreCase(aotEnabled.getValue());

        if (isAot) {
            return ConfigValue.builder()
                    .withName(name)
                    .withValue("aot-jar")
                    .withConfigSourceName("JarTypeDefaultValueConfigInterceptor")
                    .build();
        }

        return configValue;
    }
}
