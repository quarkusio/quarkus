package io.quarkus.bootstrap.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Collection factory to support {@link Mappable} implementations.
 */
public interface MappableCollectionFactory {

    /**
     * Returns a default instance of the collection factory that creates {@link HashMap} and {@link ArrayList}
     * instances.
     *
     * @return default instance of the collection factory
     */
    static MappableCollectionFactory defaultInstance() {
        return new MappableCollectionFactory() {
            @Override
            public Map<String, Object> newMap() {
                return new HashMap<>();
            }

            @Override
            public Map<String, Object> newMap(int initialCapacity) {
                return new HashMap<>(initialCapacity);
            }

            @Override
            public Collection<Object> newCollection() {
                return new ArrayList<>();
            }

            @Override
            public Collection<Object> newCollection(int initialCapacity) {
                return new ArrayList<>(initialCapacity);
            }
        };
    }

    /**
     * Creates a new {@link Map} instance.
     *
     * @return an instance of a map
     */
    Map<String, Object> newMap();

    /**
     * Creates a new {@link Map} instance with a specific initial capacity.
     *
     * @param initialCapacity initial map capacity
     * @return an instance of a map
     */
    Map<String, Object> newMap(int initialCapacity);

    /**
     * Creates a new {@link Collection} instance.
     *
     * @return an instance of a collection
     */
    Collection<Object> newCollection();

    /**
     * Creates a new {@link Collection} instance with a specific initial capacity.
     *
     * @param initialCapacity initial collection capacity
     * @return an instance of a collection
     */
    Collection<Object> newCollection(int initialCapacity);
}
