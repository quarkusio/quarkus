package io.quarkus.rest.data.panache.runtime.hal;

import java.util.Collection;

public class HalCollectionWrapper {

    private final Collection<Object> collection;

    private final Class<?> elementType;

    private final String collectionName;

    public HalCollectionWrapper(Collection<Object> collection, Class<?> elementType, String collectionName) {
        this.collection = collection;
        this.elementType = elementType;
        this.collectionName = collectionName;
    }

    public Collection<Object> getCollection() {
        return collection;
    }

    public Class<?> getElementType() {
        return elementType;
    }

    public String getCollectionName() {
        return collectionName;
    }
}
