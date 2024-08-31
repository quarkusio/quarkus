package io.quarkus.annotation.processor.documentation.config.discovery;

import io.quarkus.annotation.processor.documentation.config.model.Deprecation;
import io.quarkus.annotation.processor.documentation.config.model.SourceType;
import io.quarkus.annotation.processor.documentation.config.util.TypeUtil;
import io.quarkus.annotation.processor.util.Strings;

public class DiscoveryConfigProperty {

    private final String path;
    private final String sourceClass;
    private final String sourceName;
    private final SourceType sourceType;
    private final String defaultValue;
    private final String defaultValueForDoc;
    private final Deprecation deprecation;
    private final String mapKey;
    private final boolean unnamedMapKey;
    private final ResolvedType type;
    private final boolean converted;
    private final boolean enforceHyphenateEnumValue;
    private final boolean section;
    private final boolean sectionGenerated;

    public DiscoveryConfigProperty(String path, String sourceClass, String sourceName, SourceType sourceType,
            String defaultValue,
            String defaultValueForDoc, Deprecation deprecation, String mapKey, boolean unnamedMapKey,
            ResolvedType type, boolean converted, boolean enforceHyphenateEnumValue,
            boolean section, boolean sectionGenerated) {
        this.path = path;
        this.sourceClass = sourceClass;
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.defaultValue = defaultValue;
        this.defaultValueForDoc = defaultValueForDoc;
        this.deprecation = deprecation;
        this.mapKey = mapKey;
        this.unnamedMapKey = unnamedMapKey;
        this.type = type;
        this.converted = converted;
        this.enforceHyphenateEnumValue = enforceHyphenateEnumValue;
        this.section = section;
        this.sectionGenerated = sectionGenerated;
    }

    public String getPath() {
        return path;
    }

    public String getSourceClass() {
        return sourceClass;
    }

    public String getSourceName() {
        return sourceName;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDefaultValueForDoc() {
        return defaultValueForDoc;
    }

    public Deprecation getDeprecation() {
        return deprecation;
    }

    public boolean isDeprecated() {
        return deprecation != null;
    }

    public String getMapKey() {
        return mapKey;
    }

    public boolean isUnnamedMapKey() {
        return unnamedMapKey;
    }

    public ResolvedType getType() {
        return type;
    }

    public boolean isConverted() {
        return converted;
    }

    public boolean isEnforceHyphenateEnumValue() {
        return enforceHyphenateEnumValue;
    }

    public boolean isSection() {
        return section;
    }

    public boolean isSectionGenerated() {
        return sectionGenerated;
    }

    public String toString() {
        return toString("");
    }

    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix + "name = " + path + "\n");
        sb.append(prefix + "sourceClass = " + sourceClass + "\n");
        sb.append(prefix + "sourceName = " + sourceName + "\n");
        sb.append(prefix + "type = " + type + "\n");
        if (defaultValue != null) {
            sb.append(prefix + "defaultValue = " + defaultValue + "\n");
        }
        if (defaultValueForDoc != null) {
            sb.append(prefix + "defaultValueForDoc = " + defaultValueForDoc + "\n");
        }
        if (deprecation != null) {
            sb.append(prefix + "deprecated = true\n");
        }
        if (mapKey != null) {
            sb.append(prefix + "mapKey = " + mapKey + "\n");
        }
        if (unnamedMapKey) {
            sb.append(prefix + "unnamedMapKey = true\n");
        }
        if (converted) {
            sb.append(prefix + "converted = true\n");
        }

        return sb.toString();
    }

    public static Builder builder(String sourceClass, String sourceName, SourceType sourceType, ResolvedType type) {
        return new Builder(sourceClass, sourceName, sourceType, type);
    }

    public static class Builder {

        private String name;
        private final String sourceClass;
        private final String sourceName;
        private final SourceType sourceType;
        private final ResolvedType type;
        private String defaultValue;
        private String defaultValueForDoc;
        private Deprecation deprecation;
        private String mapKey;
        private boolean unnamedMapKey = false;
        private boolean converted = false;
        private boolean enforceHyphenateEnumValue = false;
        private boolean section = false;
        private boolean sectionGenerated = false;

        public Builder(String sourceClass, String sourceName, SourceType sourceType, ResolvedType type) {
            this.sourceClass = sourceClass;
            this.sourceName = sourceName;
            this.sourceType = sourceType;
            this.type = type;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder defaultValueForDoc(String defaultValueForDoc) {
            this.defaultValueForDoc = defaultValueForDoc;
            return this;
        }

        public Builder deprecated(String since, String replacement, String reason) {
            this.deprecation = new Deprecation(since, replacement, reason);
            return this;
        }

        public Builder mapKey(String mapKey) {
            this.mapKey = mapKey;
            return this;
        }

        public Builder unnamedMapKey() {
            this.unnamedMapKey = true;
            return this;
        }

        public Builder converted() {
            this.converted = true;
            return this;
        }

        public Builder enforceHyphenateEnumValues() {
            this.enforceHyphenateEnumValue = true;
            return this;
        }

        public Builder section(boolean generated) {
            this.section = true;
            this.sectionGenerated = generated;
            return this;
        }

        public DiscoveryConfigProperty build() {
            if (type.isPrimitive() && defaultValue == null) {
                defaultValue = TypeUtil.getPrimitiveDefaultValue(type.qualifiedName());
            }
            if (type.isDuration() && !Strings.isBlank(defaultValue)) {
                defaultValue = TypeUtil.normalizeDurationValue(defaultValue);
            }

            return new DiscoveryConfigProperty(name, sourceClass, sourceName, sourceType, defaultValue, defaultValueForDoc,
                    deprecation, mapKey, unnamedMapKey, type, converted, enforceHyphenateEnumValue, section, sectionGenerated);
        }
    }
}
