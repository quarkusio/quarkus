package io.quarkus.vertx.http.runtime.devmode;

import io.smallrye.config.ConfigValue;

public class ConfigDescription implements Comparable<ConfigDescription> {
    private String name;
    private String description;
    private String defaultValue;
    private ConfigValue configValue;
    private boolean autoFromDevServices = false;

    public ConfigDescription() {
    }

    public ConfigDescription(final String name, final String description, final String defaultValue,
            final boolean autoFromDevServices) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.autoFromDevServices = autoFromDevServices;
    }

    public ConfigDescription(
            final String name,
            final String description,
            final String defaultValue,
            final ConfigValue configValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.configValue = configValue;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(final String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public ConfigValue getConfigValue() {
        return configValue;
    }

    public void setConfigValue(final ConfigValue configValue) {
        this.configValue = configValue;
    }

    public boolean isAutoFromDevServices() {
        return autoFromDevServices;
    }

    public void setAutoFromDevServices(boolean autoFromDevServices) {
        this.autoFromDevServices = autoFromDevServices;
    }

    @Override
    public int compareTo(ConfigDescription o) {
        int ordinal = Integer.compare(o.configValue.getConfigSourceOrdinal(), this.configValue.getConfigSourceOrdinal());
        if (ordinal == 0) {
            return this.configValue.getName().compareTo(o.configValue.getName());
        }
        return ordinal;
    }
}
