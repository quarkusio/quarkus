package io.quarkus.deployment.configuration.matching;

import java.lang.reflect.Field;

import io.quarkus.deployment.configuration.definition.ClassDefinition;

/**
 * A container for a configuration key path.
 */
public abstract class Container {
    Container() {
    }

    /**
     * Get the parent container, or {@code null} if the container is a root. Presently only
     * field containers may be roots.
     *
     * @return the parent container
     */
    public abstract Container getParent();

    /**
     * Find the field that will ultimately hold this value.
     *
     * @return the field (must not be {@code null})
     */
    public final Field findField() {
        return getClassMember().getField();
    }

    /**
     * Find the enclosing class definition that will ultimately hold this value.
     *
     * @return the class definition (must not be {@code null})
     */
    public final ClassDefinition findEnclosingClass() {
        return getClassMember().getEnclosingDefinition();
    }

    /**
     * Find the enclosing class member.
     *
     * @return the enclosing class member
     */
    public abstract ClassDefinition.ClassMember getClassMember();

    /**
     * Get the combined name of this item.
     *
     * @return the combined name (must not be {@code null})
     */
    public final String getCombinedName() {
        return getCombinedName(new StringBuilder()).toString();
    }

    abstract StringBuilder getCombinedName(StringBuilder sb);

    public final String getPropertyName() {
        return getPropertyName(new StringBuilder()).toString();
    }

    abstract StringBuilder getPropertyName(StringBuilder sb);
}
