package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.jandex.Type;

/**
 * Attempts to register a complete type hierarchy for reflection.
 * <p>
 * This is intended to be used to register types that are going to be serialized,
 * e.g. by Jackson or some other JSON mapper.
 * <p>
 * This will do 'smart discovery' and in addition to registering the type itself it will also attempt to
 * register the following:
 * <p>
 * - Superclasses
 * - Component types of collections
 * - Types used in bean properties if (if method reflection is enabled)
 * - Field types (if field reflection is enabled)
 * <p>
 * This discovery is applied recursively, so any additional types that are registered will also have their dependencies
 * discovered
 */
public final class ReflectiveHierarchyBuildItem extends MultiBuildItem {

    private final Type type;

    public ReflectiveHierarchyBuildItem(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
