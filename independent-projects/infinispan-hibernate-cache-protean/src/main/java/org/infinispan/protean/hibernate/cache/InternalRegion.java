package org.infinispan.protean.hibernate.cache;

import java.util.Comparator;

interface InternalRegion {

    boolean checkValid();

    void beginInvalidation();

    void endInvalidation();

    long getLastRegionInvalidation();

    String getName();

    void clear();

    Comparator<Object> getComparator(String subclass);

    void addComparator(String name, Comparator<Object> comparator);

}
