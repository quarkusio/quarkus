package io.quarkus.hibernate.orm.runtime.cache.local;

import java.util.Comparator;

interface InternalRegion {

    boolean checkValid();

    void beginInvalidation();

    void endInvalidation();

    long getLastRegionInvalidation();

    String getName();

    void clear();

    Comparator<?> getComparator(String subclass);

    void addComparator(String name, Comparator<?> comparator);

}
