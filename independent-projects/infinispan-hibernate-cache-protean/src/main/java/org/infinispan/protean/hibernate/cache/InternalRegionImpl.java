package org.infinispan.protean.hibernate.cache;

import org.hibernate.cache.spi.Region;
import org.jboss.logging.Logger;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

final class InternalRegionImpl implements InternalRegion {

    private static final Logger log = Logger.getLogger(InternalRegionImpl.class);

    private final Region region;

    private volatile long lastRegionInvalidation = Long.MIN_VALUE;
    private int invalidations = 0;

    private final Map<String, Comparator<Object>> comparatorsByType = new HashMap<>();

    InternalRegionImpl(Region region) {
        this.region = region;
    }

    @Override
    public boolean checkValid() {
        return lastRegionInvalidation != Long.MAX_VALUE;
    }

    @Override
    public void beginInvalidation() {
        if (log.isTraceEnabled()) {
            log.trace("Begin invalidating region: " + region.getName());
        }
        synchronized (this) {
            lastRegionInvalidation = Long.MAX_VALUE;
            ++invalidations;
        }
    }

    @Override
    public void endInvalidation() {
        synchronized (this) {
            if (--invalidations == 0) {
                lastRegionInvalidation = region.getRegionFactory().nextTimestamp();
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("End invalidating region: " + region.getName());
        }
    }

    @Override
    public long getLastRegionInvalidation() {
        return lastRegionInvalidation;
    }

    @Override
    public String getName() {
        return region.getName();
    }

    @Override
    public void clear() {
        region.clear();
    }

    @Override
    public Comparator<Object> getComparator(String subclass) {
        return comparatorsByType.get(subclass);
    }

    @Override
    public void addComparator(String name, Comparator<Object> comparator) {
        comparatorsByType.put(name, comparator);
    }

}
