package io.quarkus.annotation.processor.documentation.config.discovery;

import io.quarkus.annotation.processor.documentation.config.util.TypeUtil;
import io.quarkus.annotation.processor.util.Strings;

public class DiscoveryConfigProperty {

    private final String path;
    private final String sourceName;
    private final String defaultValue;
    private final String defaultValueForDoc;
    private final boolean deprecated;
    private final String mapKey;
    private final boolean unnamedMapKey;
    private final String description;
    private final String since;
    private final String rawJavadoc;
    private final ResolvedType type;
    private final boolean converted;
    private final ParsedJavadocSection section;

    public DiscoveryConfigProperty(String path, String sourceName, String defaultValue, String defaultValueForDoc,
            boolean deprecated, String mapKey, boolean unnamedMapKey,
            String description, String since, String rawJavadoc, ResolvedType type, boolean converted,
            ParsedJavadocSection section) {
        this.path = path;
        this.sourceName = sourceName;
        this.defaultValue = defaultValue;
        this.defaultValueForDoc = defaultValueForDoc;
        this.deprecated = deprecated;
        this.mapKey = mapKey;
        this.unnamedMapKey = unnamedMapKey;
        this.description = description;
        this.since = since;
        this.rawJavadoc = rawJavadoc;
        this.type = type;
        this.converted = converted;
        this.section = section;
    }

    public String getPath() {
        return path;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDefaultValueForDoc() {
        return defaultValueForDoc;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public String getMapKey() {
        return mapKey;
    }

    public boolean isUnnamedMapKey() {
        return unnamedMapKey;
    }

    public String getDescription() {
        return description;
    }

    public String getSince() {
        return since;
    }

    public String getRawJavadoc() {
        return rawJavadoc;
    }

    public ResolvedType getType() {
        return type;
    }

    public boolean isConverted() {
        return converted;
    }

    public ParsedJavadocSection getSection() {
        return section;
    }

    public boolean isSection() {
        return section != null;
    }

    public String toString() {
        return toString("");
    }

    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix + "name = " + path + "\n");
        sb.append(prefix + "sourceName = " + sourceName + "\n");
        sb.append(prefix + "type = " + type + "\n");
        if (defaultValue != null) {
            sb.append(prefix + "defaultValue = " + defaultValue + "\n");
        }
        if (defaultValueForDoc != null) {
            sb.append(prefix + "defaultValueForDoc = " + defaultValueForDoc + "\n");
        }
        if (deprecated) {
            sb.append(prefix + "deprecated = true\n");
        }
        if (mapKey != null) {
            sb.append(prefix + "mapKey = " + mapKey + "\n");
        }
        if (unnamedMapKey) {
            sb.append(prefix + "unnamedMapKey = true\n");
        }
        if (description != null && !description.isBlank()) {
            sb.append(prefix + "description = " + description.split("\n")[0] + "...\n");
        }
        if (since != null) {
            sb.append(prefix + "since = " + since + "\n");
        }
        if (converted) {
            sb.append(prefix + "converted = true\n");
        }
        if (section != null) {
            sb.append(prefix + "section title = " + section.title() + "\n");
        }

        return sb.toString();
    }

    public static Builder builder(String sourceName, ResolvedType type) {
        return new Builder(sourceName, type);
    }

    public static class Builder {

        private String name;
        private final String sourceName;
        private final ResolvedType type;
        private String defaultValue;
        private String defaultValueForDoc;
        private boolean deprecated = false;
        private String mapKey;
        private boolean unnamedMapKey = false;
        private String description;
        private String since;
        private String rawJavadoc;
        private boolean converted;
        private ParsedJavadocSection section;

        public Builder(String sourceName, ResolvedType type) {
            this.sourceName = sourceName;
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

        public Builder deprecated() {
            this.deprecated = true;
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

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder since(String since) {
            this.since = since;
            return this;
        }

        public Builder rawJavadoc(String rawJavadoc) {
            this.rawJavadoc = rawJavadoc;
            return this;
        }

        public Builder converted() {
            this.converted = true;
            return this;
        }

        public Builder section(ParsedJavadocSection section) {
            this.section = section;
            return this;
        }

        public DiscoveryConfigProperty build() {
            if (type.isPrimitive() && defaultValue == null) {
                defaultValue = TypeUtil.getPrimitiveDefaultValue(type.qualifiedName());
            }
            if (type.isDuration() && !Strings.isBlank(defaultValue)) {
                defaultValue = TypeUtil.normalizeDurationValue(defaultValue);
            }

            return new DiscoveryConfigProperty(name, sourceName, defaultValue, defaultValueForDoc, deprecated,
                    mapKey, unnamedMapKey, description, since, rawJavadoc, type, converted, section);
        }
    }
}
