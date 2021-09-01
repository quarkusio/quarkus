package io.quarkus.deployment.builditem;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.annotations.ConfigPhase;

/**
 * A build item that is not part of the standard build, but is only used to generate
 * example config files and docs
 */
public final class ConfigDescriptionBuildItem extends MultiBuildItem implements Comparable<ConfigDescriptionBuildItem> {

    private final String propertyName;
    private final Class<?> type;
    private final String defaultValue;
    private final String docs;
    private final String valueTypeName;
    private final List<String> allowedValues;
    private final ConfigPhase configPhase;

    public ConfigDescriptionBuildItem(String propertyName, Class<?> type, String defaultValue, String docs,
            String valueTypeName, List<String> allowedValues, ConfigPhase configPhase) {
        this.propertyName = propertyName;
        this.type = type;
        this.defaultValue = defaultValue;
        this.docs = docs;
        this.valueTypeName = valueTypeName;
        this.allowedValues = allowedValues;
        this.configPhase = configPhase;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Class<?> getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDocs() {
        return docs;
    }

    public String getValueTypeName() {
        return valueTypeName;
    }

    public List<String> getAllowedValues() {
        return allowedValues;
    }

    public ConfigPhase getConfigPhase() {
        return configPhase;
    }

    public boolean hasAllowedValues() {
        return allowedValues != null && !allowedValues.isEmpty();
    }

    @Override
    public int compareTo(ConfigDescriptionBuildItem o) {
        return propertyName.compareTo(o.propertyName);
    }
}
