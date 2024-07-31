package io.quarkus.annotation.processor.documentation.config.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public sealed abstract class AbstractConfigItem implements Comparable<AbstractConfigItem>
        permits ConfigProperty, ConfigSection {

    private final String sourceClass;
    private final String sourceName;
    private final String path;

    private final String type;

    public AbstractConfigItem(String sourceClass, String sourceName, String path, String type) {
        this.sourceClass = sourceClass;
        this.sourceName = sourceName;
        this.path = path;
        this.type = type;
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

    public abstract boolean isSection();
}
