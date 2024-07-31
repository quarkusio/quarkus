package io.quarkus.annotation.processor.documentation.config.model;

import java.util.List;

public final class ConfigProperty extends AbstractConfigItem {

    private final ConfigPhase phase;
    private final List<String> additionalPaths;
    private final String environmentVariable;

    private final String typeDescription;
    private final boolean map;
    private final boolean list;
    private final boolean optional;
    private final String mapKey;
    private final boolean unnamedMapKey;
    private final boolean withinMap;
    private final boolean converted;
    private final boolean isEnum;
    private final EnumAcceptedValues enumAcceptedValues;

    private final String defaultValue;

    private final String javadocSiteLink;

    private final boolean deprecated;

    public ConfigProperty(ConfigPhase phase, String sourceClass, String sourceName, String path, List<String> additionalPaths,
            String environmentVariable, String type, String typeDescription, boolean map, boolean list, boolean optional,
            String mapKey, boolean unnamedMapKey, boolean withinMap, boolean converted, boolean isEnum,
            EnumAcceptedValues enumAcceptedValues,
            String defaultValue, String javadocSiteLink,
            boolean deprecated) {
        super(sourceClass, sourceName, path, typeDescription);
        this.phase = phase;
        this.additionalPaths = additionalPaths;
        this.environmentVariable = environmentVariable;
        this.typeDescription = typeDescription;
        this.map = map;
        this.list = list;
        this.optional = optional;
        this.mapKey = mapKey;
        this.unnamedMapKey = unnamedMapKey;
        this.withinMap = withinMap;
        this.converted = converted;
        this.isEnum = isEnum;
        this.enumAcceptedValues = enumAcceptedValues;
        this.defaultValue = defaultValue;
        this.javadocSiteLink = javadocSiteLink;
        this.deprecated = deprecated;
    }

    public ConfigPhase getPhase() {
        return phase;
    }

    public List<String> getAdditionalPaths() {
        return additionalPaths;
    }

    public String getEnvironmentVariable() {
        return environmentVariable;
    }

    public String getTypeDescription() {
        return typeDescription;
    }

    public boolean isMap() {
        return map;
    }

    public boolean isList() {
        return list;
    }

    public boolean isOptional() {
        return optional;
    }

    public String getMapKey() {
        return mapKey;
    }

    public boolean isUnnamedMapKey() {
        return unnamedMapKey;
    }

    public boolean isWithinMap() {
        return withinMap;
    }

    public boolean isConverted() {
        return converted;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public EnumAcceptedValues getEnumAcceptedValues() {
        return enumAcceptedValues;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getJavadocSiteLink() {
        return javadocSiteLink;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public boolean isSection() {
        return false;
    }

    @Override
    public int compareTo(AbstractConfigItem o) {
        if (o instanceof ConfigSection) {
            return -1;
        }

        ConfigProperty other = (ConfigProperty) o;

        if (isWithinMap()) {
            if (other.isWithinMap()) {
                return ConfigPhase.COMPARATOR.compare(phase, other.getPhase());
            }
            return 1;
        } else if (other.isWithinMap()) {
            return -1;
        }

        return ConfigPhase.COMPARATOR.compare(phase, other.getPhase());
    }
}
