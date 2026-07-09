package io.quarkus.deployment.key;

import java.lang.reflect.Field;

/**
 * Describes a single {@code @Key}-annotated field on a {@link io.quarkus.builder.item.MultiBuildItem} class.
 *
 * @param keyType the key family type (any class identifying this key dimension)
 * @param field the reflective field handle (already made accessible)
 */
public record KeyDescriptor(Class<?> keyType, Field field) {

    public String getValue(Object instance) {
        try {
            return (String) field.get(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot read @Key field " + field + " on " + instance.getClass(), e);
        }
    }
}
