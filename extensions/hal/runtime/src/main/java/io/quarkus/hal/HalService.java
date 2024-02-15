package io.quarkus.hal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service with Hal utilities. This service is used by the Resteasy Links, Resteasy Reactive Links and the
 * Rest Data Panache extensions.
 */
@SuppressWarnings("unused")
public abstract class HalService {

    private static final String SELF_REF = "self";

    /**
     * Wrap a collection of objects into a Hal collection wrapper by resolving the Hal links.
     * The Hal collection wrapper is then serialized by either json or jackson.
     *
     * @param collection The collection of objects to wrap.
     * @param collectionName The name that will include the collection of objects within the `_embedded` Hal object.
     * @param entityClass The class of the objects in the collection. If null, it will not resolve the links for these objects.
     * @return The Hal collection wrapper instance.
     */
    public <T> HalCollectionWrapper<T> toHalCollectionWrapper(Collection<T> collection, String collectionName,
            Class<?> entityClass) {
        List<HalEntityWrapper<T>> items = new ArrayList<>();
        for (T entity : collection) {
            items.add(toHalWrapper(entity));
        }

        Map<String, HalLink> classLinks = Collections.emptyMap();
        if (entityClass != null) {
            classLinks = getClassLinks(entityClass);
        }

        return new HalCollectionWrapper<>(items, collectionName, classLinks);
    }

    /**
     * Wrap an entity into a Hal instance by including the entity itself and the Hal links.
     *
     * @param entity The entity to wrap.
     * @return The Hal entity wrapper.
     */
    public <T> HalEntityWrapper<T> toHalWrapper(T entity) {
        return new HalEntityWrapper<>(entity, getInstanceLinks(entity));
    }

    /**
     * Get the HREF link with reference `self` from the Hal links of the entity instance.
     *
     * @param entity The entity instance where to get the Hal links.
     * @return the HREF link with rel `self`.
     */
    public String getSelfLink(Object entity) {
        HalLink halLink = getInstanceLinks(entity).get(SELF_REF);
        if (halLink != null) {
            return halLink.getHref();
        }

        return null;
    }

    /**
     * Get the Hal links using the entity type class.
     *
     * @param entityClass The entity class to get the Hal links.
     * @return a map with the Hal links which keys are the rel attributes, and the values are the href attributes.
     */
    protected abstract Map<String, HalLink> getClassLinks(Class<?> entityClass);

    /**
     * Get the Hal links using the entity instance.
     *
     * @param entity the Object instance.
     * @return a map with the Hal links which keys are the rel attributes, and the values are the href attributes.
     */
    protected abstract Map<String, HalLink> getInstanceLinks(Object entity);
}
