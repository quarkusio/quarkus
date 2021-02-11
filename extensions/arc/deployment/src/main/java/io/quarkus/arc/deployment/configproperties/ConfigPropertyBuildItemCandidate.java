package io.quarkus.arc.deployment.configproperties;

import org.jboss.jandex.Type;

public class ConfigPropertyBuildItemCandidate {

    private final String fieldName;

    private final String configPropertyName;

    private final Type configPropertyType;

    public ConfigPropertyBuildItemCandidate(String fieldName, String configPropertyName, Type configPropertyType) {
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

    public Type getConfigPropertyType() {
        return configPropertyType;
    }
}
