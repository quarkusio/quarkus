package io.quarkus.builder.item;

import java.lang.reflect.Modifier;

/**
 * A build item which can be produced or consumed. Any item
 * which implements {@link AutoCloseable} will be automatically closed when the build
 * is completed, unless it is explicitly marked as a final build result in which case closure is
 * the responsibility of whomever invoked the build execution.
 * <p>
 * Resources should be fine-grained as possible, ideally describing only one aspect of the build process.
 */
public abstract class BuildItem {
    BuildItem() {
        final Class<? extends BuildItem> clazz = getClass();
        if (clazz.getTypeParameters().length != 0) {
            throw new IllegalArgumentException(
                    "A generic type is not allowed here; try creating a subclass with concrete type arguments instead: "
                            + getClass());
        }
        if (!Modifier.isFinal(clazz.getModifiers())) {
            throw new IllegalArgumentException("Build item class must be leaf (final) types: " + getClass());
        }
    }
}
