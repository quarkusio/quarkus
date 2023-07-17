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
public class HalCollectionWrapper extends HalWrapper {

    private final Collection<HalEntityWrapper> collection;
    private final String collectionName;

    public HalCollectionWrapper(Collection<HalEntityWrapper> collection, String collectionName, Link... links) {
        this(collection, collectionName, new HashMap<>());

        addLinks(links);
    }

    public HalCollectionWrapper(Collection<HalEntityWrapper> collection, String collectionName, Map<String, HalLink> links) {
        super(links);

        this.collection = collection;
        this.collectionName = collectionName;
    }

    public Collection<HalEntityWrapper> getCollection() {
        return collection;
    }

    public String getCollectionName() {
        return collectionName;
    }

}
