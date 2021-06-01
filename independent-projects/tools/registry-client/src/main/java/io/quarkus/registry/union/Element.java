package io.quarkus.registry.union;

import java.util.Collection;

public interface Element<M> {

    /**
     * Element key.
     *
     * @return element key
     */
    Object key();

    /**
     * Members that provide the element.
     *
     * @return members that provide the element
     */
    Collection<Member<M>> members();
}
