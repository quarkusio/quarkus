package io.quarkus.registry.union;

import java.util.Collection;

public interface Member extends ElementCatalog {

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
     * The very first union the member joined.
     *
     * @return the very first union the member joined the union
     */
    Union initialUnion();

    /**
     * Unions this member belongs to.
     *
     * @return unions this member belongs to
     */
    Collection<UnionVersion> unions();

    /**
     * Checks whether this member contains all the element keys.
     *
     * @param elementKeys element keys
     * @return true if the member contains all the element keys, otherwise - false
     */
    boolean containsAll(Collection<Object> elementKeys);
}
