package io.quarkus.arc.impl;

import java.util.Iterator;
import java.util.Set;

/**
 * Inspired from Hibernate Validator: a couple tricks
 * to minimize retained memory from long living, small collections.
 */
final class CollectionHelpers {

    CollectionHelpers() {
        //do not instantiate
    }

    @SuppressWarnings("unchecked")
    static <T> Set<T> toImmutableSmallSet(Set<T> set) {
        if (set == null)
            return null;
        switch (set.size()) {
            case 0:
                return Set.of();
            case 1:
                return Set.of(set.iterator().next());
            case 2:
                Iterator<T> it = set.iterator();
                return Set.of(it.next(), it.next());
            default:
                return (Set<T>) Set.of(set.toArray());
        }
    }

}
