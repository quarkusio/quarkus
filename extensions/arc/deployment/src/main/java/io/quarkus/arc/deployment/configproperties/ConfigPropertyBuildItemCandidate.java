package io.quarkus.arc.deployment.configproperties;

public class ConfigPropertyBuildItemCandidate {

    private final String fieldName;

    private final String configPropertyName;

    private final String configPropertyType;

    public ConfigPropertyBuildItemCandidate(String fieldName, String configPropertyName, String configPropertyType) {
        this.fieldName = fieldName;
        this.configPropertyName = configPropertyName;
        this.configPropertyType = configPropertyType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getConfigPropertyName() {
        return configPropertyName;
    }

    public String getConfigPropertyType() {
        return configPropertyType;
    }
}
