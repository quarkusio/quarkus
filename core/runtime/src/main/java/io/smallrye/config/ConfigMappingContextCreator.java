package io.smallrye.config;

import io.smallrye.config.ConfigMappingInterface.KebabNamingStrategy;

/**
 * TODO: this is a temporary class, it needs to go away.
 */
@Deprecated(forRemoval = true)
public final class ConfigMappingContextCreator {

    public static ConfigMappingContext createConfigMappingContext(SmallRyeConfig config) {
        ConfigMappingContext configMappingContext = new ConfigMappingContext(config);
        configMappingContext.applyNamingStrategy(new KebabNamingStrategy());
        return configMappingContext;
    }
}
