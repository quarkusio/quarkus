package io.quarkus.rest.runtime.model;

import java.util.Comparator;

public interface HasPriority {

    Integer getPriority();

    /**
     * This comparator is used when a TreeMap is employed to order objects that have priority
     * because TreeMap will only keep one key if multiple keys compare to the same value
     */
    class TreeMapComparator implements Comparator<HasPriority> {

        public static final TreeMapComparator INSTANCE = new TreeMapComparator();

        @Override
        public int compare(HasPriority o1, HasPriority o2) {
            int priorityCompare = o1.getPriority().compareTo(o2.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return Integer.compare(o1.hashCode(), o2.hashCode());
        }
    }
}
