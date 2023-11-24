package io.quarkus.devui.runtime.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PermissionDescription implements Comparable<PermissionDescription> {

    private String propertyKey;
    private ConfigDescription configDescription;
    private String value;
    private String configId;
    private AuthFieldType type;

    public static Pattern getPattern(String propertyKey) {

        for (AuthFieldType value : AuthFieldType.values()) {
            Pattern pattern = value.getPattern();
            Matcher matcher = pattern.matcher(propertyKey);
            if (matcher.matches()) {
                return pattern;
            }
        }
        return null;
    }

    public PermissionDescription() {
    }

    public PermissionDescription(String propertyKey, String value, ConfigDescription configDescription) {
        this.configDescription = configDescription;
        this.value = value;
        this.propertyKey = propertyKey;

        for (AuthFieldType authFieldType : AuthFieldType.values()) {
            Pattern pattern = authFieldType.getPattern();
            Matcher matcher = pattern.matcher(propertyKey);
            if (matcher.matches()) {
                this.type = authFieldType;
                this.configId = matcher.group(1);
                break;
            }
        }

        if (this.type == null) {
            throw new IllegalArgumentException("Cannot find a valid type for " + configDescription.getName());
        }
    }

    public String getValue() {
        return value;
    }

    public ConfigDescription getConfigDescription() {
        return configDescription;
    }

    public void setConfigDescription(ConfigDescription configDescription) {
        this.configDescription = configDescription;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public AuthFieldType getType() {
        return type;
    }

    public AuthFieldType getFieldType() {
        return type;
    }

    public AuthFieldType.AuthConfigType getConfigType() {
        return this.type.authConfigType;
    }

    public String getConfigId() {
        return this.configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public void setType(AuthFieldType type) {
        this.type = type;
    }

    @Override
    public int compareTo(PermissionDescription permissionDescription) {
        return this.configId.compareTo(permissionDescription.configId);
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public void setPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }
}
