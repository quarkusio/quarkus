package io.quarkus.resteasy.reactive.client.runtime;

import java.util.Collection;

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

    private ToObjectArray() {
    }
}
