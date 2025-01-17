package io.quarkus.jaxrs.client.reactive.runtime;

import java.util.Collection;
import java.util.Optional;

/**
 * used by query param handling mechanism, in generated code
 */
@SuppressWarnings("unused")
public class ToObjectArray {

    public static Object[] collection(Collection<?> collection) {
        return collection.toArray();
    }

    public static Object[] value(Object value) {
        return new Object[] { value };
    }

    public static Object[] optional(Optional<?> optional) {
        return optional.isPresent() ? new Object[] { optional.get() } : new Object[] {};
    }

    private ToObjectArray() {
    }
}
