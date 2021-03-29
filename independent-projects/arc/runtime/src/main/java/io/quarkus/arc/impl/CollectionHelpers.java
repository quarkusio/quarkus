package io.quarkus.arc.impl;

import java.util.Collections;
import java.util.Set;

/**
 * Inspired from Hibernate Validator: a couple tricks
 * to minimize retained memory from long living, small collections.
 */
final class CollectionHelpers {

    CollectionHelpers() {
        //do not instantiate
    }

    static <T> Set<T> toImmutableSmallSet(Set<T> set) {
        if (set == null)
            return null;
        switch (set.size()) {
            case 0:
                return Collections.emptySet();
            case 1:
                return Collections.singleton(set.iterator().next());
            default:
                return Collections.unmodifiableSet(set);
        }
    }

}
