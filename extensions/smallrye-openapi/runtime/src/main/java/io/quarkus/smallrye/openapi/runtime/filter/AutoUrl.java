package io.quarkus.smallrye.openapi.runtime.filter;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Create a URL from a config, or a default value
 */
public class AutoUrl {

    private String defaultValue;
    private String configKey;
    private String path;

    public AutoUrl() {
    }

    public AutoUrl(String defaultValue, String configKey, String path) {
        this.defaultValue = defaultValue;
        this.configKey = configKey;
        this.path = path;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFinalUrlValue() {
        Config c = ConfigProvider.getConfig();

        String u = c.getOptionalValue(this.configKey, String.class).orElse(defaultValue);

        if (u != null && path != null && !u.endsWith(path)) {
            u = u + path;
        }

        return u;
    }

}
