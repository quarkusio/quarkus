package io.quarkus.arc.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Sets {

    private Sets() {
    }

    /**
     * Unlike {@link Set#of(Object...)} this method does not throw an {@link IllegalArgumentException} if there are duplicate
     * elements.
     *
     * @param <E>
     * @param elements
     * @return the set
     */
    @SafeVarargs
    public static <E> Set<E> of(E... elements) {
        switch (elements.length) {
            case 0:
                return Set.of();
            case 1:
                return Set.of(elements[0]);
            case 2:
                return elements[0].equals(elements[1]) ? Set.of(elements[0]) : Set.of(elements[0], elements[1]);
            default:
                return Set.copyOf(Arrays.asList(elements));
        }
    }

    public static <E> HashSet<E> singletonHashSet(E element) {
        HashSet<E> result = new HashSet<>();
        result.add(element);
        return result;
    }
}
