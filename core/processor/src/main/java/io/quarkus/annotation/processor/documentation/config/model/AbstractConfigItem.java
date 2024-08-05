package io.quarkus.annotation.processor.documentation.config.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public sealed abstract class AbstractConfigItem implements Comparable<AbstractConfigItem>
        permits ConfigProperty, ConfigSection {

    protected final String sourceClass;
    protected final String sourceName;
    protected final String path;

    protected final String type;

    protected boolean deprecated;

    public AbstractConfigItem(String sourceClass, String sourceName, String path, String type, boolean deprecated) {
        this.sourceClass = sourceClass;
        this.sourceName = sourceName;
        this.path = path;
        this.type = type;
        this.deprecated = deprecated;
    }

    public String getSourceClass() {
        return sourceClass;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    @JsonIgnore
    public abstract boolean isSection();

    @JsonIgnore
    public abstract boolean hasDurationType();

    @JsonIgnore
    public abstract boolean hasMemorySizeType();
}
