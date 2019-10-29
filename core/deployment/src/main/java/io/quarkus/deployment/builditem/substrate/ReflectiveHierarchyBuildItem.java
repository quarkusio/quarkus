package io.quarkus.deployment.builditem.substrate;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;

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
 * 
 * @deprecated Use {@link io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem ReflectiveHierarchyBuildItem}
 *             instead.
 */
@Deprecated
public final class ReflectiveHierarchyBuildItem extends MultiBuildItem {

    private final Type type;
    private IndexView index;

    public ReflectiveHierarchyBuildItem(Type type) {
        this.type = type;
    }

    public ReflectiveHierarchyBuildItem(Type type, IndexView index) {
        this.type = type;
        this.index = index;
    }

    public Type getType() {
        return type;
    }

    public IndexView getIndex() {
        return index;
    }
}
