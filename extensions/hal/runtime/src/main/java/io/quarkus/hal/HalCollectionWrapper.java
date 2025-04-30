package io.quarkus.hal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.Link;

/**
 * The Hal collection wrapper that includes the list of Hal entities {@link HalEntityWrapper}, the collection name and the Hal
 * links.
 *
 * This type is serialized into Json using:
 * - the JSON-B serializer: {@link HalCollectionWrapperJsonbSerializer}
 * - the Jackson serializer: {@link HalCollectionWrapperJacksonSerializer}
 */
public class HalCollectionWrapper<T> extends HalWrapper {

    private final Collection<HalEntityWrapper<T>> collection;
    private final String collectionName;

    public HalCollectionWrapper(Collection<HalEntityWrapper<T>> collection, String collectionName, Link... links) {
        this(collection, collectionName, new HashMap<>());

        addLinks(links);
    }

    public HalCollectionWrapper(Collection<HalEntityWrapper<T>> collection, String collectionName, Map<String, HalLink> links) {
        super(links);

        this.collection = collection;
        this.collectionName = collectionName;
    }

    public Collection<HalEntityWrapper<T>> getCollection() {
        return collection;
    }

    public String getCollectionName() {
        return collectionName;
    }

}
