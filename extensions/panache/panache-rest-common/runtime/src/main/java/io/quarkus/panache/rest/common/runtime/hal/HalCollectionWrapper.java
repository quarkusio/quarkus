package io.quarkus.panache.rest.common.runtime.hal;

import java.util.Collection;

public class HalCollectionWrapper {

    private final Collection<Object> collection;

    private final Class<?> elementType;

    public HalCollectionWrapper(Collection<Object> collection, Class<?> elementType) {
        this.collection = collection;
        this.elementType = elementType;
    }

    public Collection<Object> getCollection() {
        return collection;
    }

    public Class<?> getElementType() {
        return elementType;
    }
}
