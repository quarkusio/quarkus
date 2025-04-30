package io.quarkus.annotation.processor.documentation.config.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public sealed abstract class AbstractConfigItem implements Comparable<AbstractConfigItem>
        permits ConfigProperty, ConfigSection {

    protected final String sourceType;
    protected final String sourceElementName;
    protected final SourceElementType sourceElementType;
    protected final Path path;

    protected final String type;

    protected Deprecation deprecation;

    public AbstractConfigItem(String sourceType, String sourceElementName, SourceElementType sourceElementType, Path path,
            String type,
            Deprecation deprecation) {
        this.sourceType = sourceType;
        this.sourceElementName = sourceElementName;
        this.sourceElementType = sourceElementType;
        this.path = path;
        this.type = type;
        this.deprecation = deprecation;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceElementName() {
        return sourceElementName;
    }

    public SourceElementType getSourceElementType() {
        return sourceElementType;
    }

    public Path getPath() {
        return path;
    }

    @Deprecated
    @JsonIgnore
    public String getPath$$bridge() {
        return path.property();
    }

    public String getType() {
        return type;
    }

    @JsonIgnore
    public boolean isDeprecated() {
        return deprecation != null;
    }

    public Deprecation getDeprecation() {
        return deprecation;
    }

    @JsonIgnore
    public abstract boolean isSection();

    @JsonIgnore
    public abstract boolean hasDurationType();

    @JsonIgnore
    public abstract boolean hasMemorySizeType();

    protected abstract void walk(ConfigItemVisitor visitor);

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public interface Path {
        String property();
    }
}
