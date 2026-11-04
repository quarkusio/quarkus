package io.quarkus.smallrye.reactivemessaging.deployment;

import java.lang.annotation.Annotation;
import java.util.Objects;

import jakarta.enterprise.util.AnnotationLiteral;

import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;

public class ConnectorAttributeLiteral extends AnnotationLiteral<ConnectorAttribute> implements ConnectorAttribute {

    private static final long serialVersionUID = 1L;

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

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public boolean hiddenFromDocumentation() {
        return hidden;
    }

    public boolean mandatory() {
        return mandatory;
    }

    public Direction direction() {
        return direction;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public boolean deprecated() {
        return deprecated;
    }

    public String alias() {
        return alias;
    }

    public String type() {
        return type;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return ConnectorAttribute.class;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof ConnectorAttribute that) {
            return Objects.equals(this.name, that.name())
                    && Objects.equals(this.description, that.description())
                    && this.hidden == that.hiddenFromDocumentation()
                    && this.mandatory == that.mandatory()
                    && Objects.equals(this.direction, that.direction())
                    && Objects.equals(this.defaultValue, that.defaultValue())
                    && this.deprecated == that.deprecated()
                    && Objects.equals(this.alias, that.alias())
                    && Objects.equals(this.type, that.type());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = 0;
        result += 127 * "name".hashCode() ^ Objects.hashCode(this.name);
        result += 127 * "description".hashCode() ^ Objects.hashCode(this.description);
        result += 127 * "hiddenFromDocumentation".hashCode() ^ Boolean.hashCode(this.hidden);
        result += 127 * "mandatory".hashCode() ^ Boolean.hashCode(this.mandatory);
        result += 127 * "direction".hashCode() ^ Objects.hashCode(this.direction);
        result += 127 * "defaultValue".hashCode() ^ Objects.hashCode(this.defaultValue);
        result += 127 * "deprecated".hashCode() ^ Boolean.hashCode(this.deprecated);
        result += 127 * "alias".hashCode() ^ Objects.hashCode(this.alias);
        result += 127 * "type".hashCode() ^ Objects.hashCode(this.type);
        return result;
    }
}
