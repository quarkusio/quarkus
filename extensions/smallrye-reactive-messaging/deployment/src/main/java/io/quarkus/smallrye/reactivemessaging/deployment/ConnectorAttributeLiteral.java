package io.quarkus.smallrye.reactivemessaging.deployment;

import java.lang.annotation.Annotation;

import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;

public class ConnectorAttributeLiteral implements ConnectorAttribute {

    private final String name;
    private final String description;
    private final boolean hidden;
    private final boolean mandatory;
    private final Direction direction;
    private final String defaultValue;
    private final boolean deprecated;
    private final String alias;
    private final String type;

    public ConnectorAttributeLiteral(String name, String description, boolean hidden, boolean mandatory,
            Direction direction, String defaultValue, boolean deprecated, String alias, String type) {
        this.name = name;
        this.description = description;
        this.hidden = hidden;
        this.mandatory = mandatory;
        this.direction = direction;
        this.defaultValue = defaultValue;
        this.deprecated = deprecated;
        this.alias = alias;
        this.type = type;
    }

    public static ConnectorAttribute create(String name, String description, boolean hidden, boolean mandatory,
            Direction direction, String defaultValue, boolean deprecated, String alias, String type) {
        return new ConnectorAttributeLiteral(name, description, hidden, mandatory, direction, defaultValue, deprecated, alias,
                type);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public boolean hiddenFromDocumentation() {
        return hidden;
    }

    @Override
    public boolean mandatory() {
        return mandatory;
    }

    @Override
    public Direction direction() {
        return direction;
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }

    @Override
    public boolean deprecated() {
        return deprecated;
    }

    @Override
    public String alias() {
        return alias;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return ConnectorAttribute.class;
    }
}
