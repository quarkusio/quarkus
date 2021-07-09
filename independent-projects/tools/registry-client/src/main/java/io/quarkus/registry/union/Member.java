package io.quarkus.registry.union;

import java.util.Collection;

public interface Member<T> extends ElementCatalog<T> {

    /**
     * Member key
     *
     * @return member key
     */
    Object key();

    /**
     * Member version
     *
     * @return member version
     */
    Object version();

    /**
     * Actual member instance.
     *
     * @return member instance or null
     */
    T getInstance();

    /**
     * Checks whether this member contains all the element keys.
     *
     * @param elementKeys element keys
     * @return true if the member contains all the element keys, otherwise - false
     */
    boolean containsAll(Collection<Object> elementKeys);
}
