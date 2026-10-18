package org.jboss.resteasy.reactive.common.model;

import java.util.Comparator;

public interface HasPriority {

    Integer priority();

    /**
     * This comparator is used when a TreeMap is employed to order objects that have priority
     * because TreeMap will only keep one key if multiple keys compare to the same value
     */
    class TreeMapComparator implements Comparator<HasPriority> {

        public static final TreeMapComparator INSTANCE = new TreeMapComparator();

        public static final TreeMapComparator REVERSED = new TreeMapComparator() {
            @Override
            public int compare(HasPriority o1, HasPriority o2) {
                return super.compare(o2, o1);
            }
        };

        @Override
        public int compare(HasPriority o1, HasPriority o2) {
            int res = o1.priority().compareTo(o2.priority());
            if (res != 0) {
                return res;
            }
            res = Integer.compare(o1.hashCode(), o2.hashCode());
            if (res != 0) {
                return res;
            }
            //what to do here
            //they are functionally equal, but we don't want one to be discarded if there is a hash collision
            return 1;
        }
    }
}
