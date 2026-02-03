package io.quarkus.arc.impl;

import java.util.HashSet;

public final class Sets {

    private Sets() {
    }

    public static <E> HashSet<E> singletonHashSet(E element) {
        HashSet<E> result = new HashSet<>();
        result.add(element);
        return result;
    }
}
