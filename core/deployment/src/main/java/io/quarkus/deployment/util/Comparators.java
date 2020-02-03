package io.quarkus.deployment.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

public class Comparators {
    private Comparators() {
    }

    /**
     * @param <T> the collection element type
     * @return a {@link Comparator} for comparing two {@link Collection}s element by element using the natural
     *         ordering of the elements.
     */
    public static <T extends Comparable<? super T>> Comparator<Collection<T>> forCollections() {
        return (Collection<T> col1, Collection<T> col2) -> {
            if (col1 == col2) {
                return 0;
            }

            Iterator<? extends T> i1 = col1.iterator();
            final Iterator<? extends T> i2 = col2.iterator();
            while (i1.hasNext() && i2.hasNext()) {
                final int cmp = i1.next().compareTo(i2.next());
                if (cmp != 0) {
                    return cmp;
                }
            }
            if (i1.hasNext()) {
                return 1;
            } else if (i2.hasNext()) {
                return -1;
            } else {
                return 0;
            }
        };
    }
}
