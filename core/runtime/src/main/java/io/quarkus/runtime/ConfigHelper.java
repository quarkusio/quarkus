package io.quarkus.runtime;

import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

public class ConfigHelper {

    private ConfigHelper() {

    }

    public static String getString(String key, String defaultValue) {
        Optional<String> val = ConfigProvider.getConfig().getOptionalValue(key, String.class);
        return val.orElse(defaultValue);
    }

    public static Integer getInteger(String key, int defaultValue) {
        Optional<Integer> val = ConfigProvider.getConfig().getOptionalValue(key, Integer.class);
        return val.orElse(defaultValue);
    }

    public static Boolean getBoolean(String key, boolean defaultValue) {
        Optional<Boolean> val = ConfigProvider.getConfig().getOptionalValue(key, Boolean.class);
        return val.orElse(defaultValue);
    }
}
