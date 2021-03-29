package org.jboss.resteasy.reactive.common.util;

import java.util.List;
import javax.ws.rs.core.MultivaluedMap;

/**
 * MultivaluedMap with extra operations
 */
public interface QuarkusMultivaluedMap<Key, Value> extends MultivaluedMap<Key, Value> {
    /**
     * Adds all elements of the given map to this map.
     * 
     * @param otherMap the map to take keys and values from
     */
    public default void addAll(MultivaluedMap<Key, Value> otherMap) {
        for (Entry<Key, List<Value>> entry : otherMap.entrySet()) {
            addAll(entry.getKey(), entry.getValue());
        }
    }
}
