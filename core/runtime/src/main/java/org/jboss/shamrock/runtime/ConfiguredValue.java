package org.jboss.shamrock.runtime;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Represents a value that can be read from MicroProfile config. It is designed to be passed
 * to Template objects, and as such can be configured at both build and run time.
 *
 * The value is determined as follows:
 *
 * - If the key is present in MPConfig at runtime, then the runtime value is used
 * - If the key is present at deployment time, then the deployment time value is used
 * - Otherwise the default value is used, or null if it was not provided
 *
 * TODO: this is a more explicit alternative to the transparent config option provided by ShamrockConfig. We should probably only have one, this is something to revisit later
 *
 */
public class ConfiguredValue {

    private static final Config config = ConfigProvider.getConfig();

    private final String key;
    private final String defaultValue;

    public ConfiguredValue(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String getValue() {
        return config.getOptionalValue(key, String.class).orElse(defaultValue);
    }

    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
