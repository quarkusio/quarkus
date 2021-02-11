package io.quarkus.rest.data.panache.runtime.hal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Link;

public class HalCollectionWrapper {

    private final Collection<Object> collection;

    private final Class<?> elementType;

    private final String collectionName;

    private final Map<String, HalLink> links = new HashMap<>();

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

    public Map<String, HalLink> getLinks() {
        return links;
    }

    public void addLinks(Link... links) {
        for (Link link : links) {
            this.links.put(link.getRel(), new HalLink(link.getUri().toString()));
        }
    }
}
