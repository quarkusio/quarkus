package io.quarkus.dev.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CurrentConfig implements Comparable<CurrentConfig> {

    public static volatile List<CurrentConfig> CURRENT = Collections.emptyList();
    public static volatile Consumer<Map<String, String>> EDITOR;

    private final String propertyName;
    private final String description;
    private final String defaultValue;
    private final String currentValue;
    private final String appPropertiesValue;

    public CurrentConfig(String propertyName, String description, String defaultValue, String currentValue,
            String appPropertiesValue) {
        this.propertyName = propertyName;
        this.description = description;
        this.defaultValue = defaultValue;
        this.currentValue = currentValue;
        this.appPropertiesValue = appPropertiesValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public String getAppPropertiesValue() {
        return appPropertiesValue;
    }

    @Override
    public int compareTo(CurrentConfig o) {
        if (appPropertiesValue == null && o.appPropertiesValue != null) {
            return 1;
        }
        if (appPropertiesValue != null && o.appPropertiesValue == null) {
            return -1;
        }

        return propertyName.compareTo(o.propertyName);
    }
}
