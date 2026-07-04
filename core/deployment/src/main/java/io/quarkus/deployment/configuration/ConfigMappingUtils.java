package io.quarkus.deployment.configuration;

import io.quarkus.deployment.util.ReflectUtil;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMappingLoader;

@Deprecated(forRemoval = true)
public class ConfigMappingUtils {
    private ConfigMappingUtils() {
        throw new UnsupportedOperationException();
    }

    @Deprecated(forRemoval = true, since = "3.25")
    public static Object newInstance(Class<?> configClass) {
        if (configClass.isAnnotationPresent(ConfigMapping.class)) {
            return ReflectUtil.newInstance(ConfigMappingLoader.ensureLoaded(configClass).implementation());
        } else {
            return ReflectUtil.newInstance(configClass);
        }
    }
}
