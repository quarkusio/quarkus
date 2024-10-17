package io.quarkus.annotation.processor.documentation.config.model;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.annotation.processor.documentation.config.util.Types;

public final class ConfigProperty extends AbstractConfigItem {

    private final ConfigPhase phase;
    private final List<PropertyPath> additionalPaths;

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

    public ConfigProperty(ConfigPhase phase, String sourceType, String sourceElementName, SourceElementType sourceElementType,
            PropertyPath path, List<PropertyPath> additionalPaths, String type, String typeDescription, boolean map,
            boolean list, boolean optional,
            String mapKey, boolean unnamedMapKey, boolean withinMap, boolean converted, @JsonProperty("enum") boolean isEnum,
            EnumAcceptedValues enumAcceptedValues,
            String defaultValue, String javadocSiteLink,
            Deprecation deprecation) {
        super(sourceType, sourceElementName, sourceElementType, path, type, deprecation);
        this.phase = phase;
        this.additionalPaths = additionalPaths != null ? Collections.unmodifiableList(additionalPaths) : List.of();
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
    }

    public ConfigPhase getPhase() {
        return phase;
    }

    public PropertyPath getPath() {
        return (PropertyPath) super.getPath();
    }

    public List<PropertyPath> getAdditionalPaths() {
        return additionalPaths;
    }

    @Deprecated
    @JsonIgnore
    public String getEnvironmentVariable() {
        return getPath().environmentVariable();
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

    @Override
    public boolean hasDurationType() {
        return Duration.class.getName().equals(type);
    }

    @Override
    public boolean hasMemorySizeType() {
        return Types.MEMORY_SIZE_TYPE.equals(type);
    }

    @Override
    protected void walk(ConfigItemVisitor visitor) {
        visitor.visit(this);
    }

    public record PropertyPath(String property, String environmentVariable) implements Path {

        @Override
        public String toString() {
            return property();
        }
    }
}
