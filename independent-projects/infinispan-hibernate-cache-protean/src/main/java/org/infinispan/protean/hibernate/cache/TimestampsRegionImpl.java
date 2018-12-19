package org.infinispan.protean.hibernate.cache;

import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.jboss.logging.Logger;

final class TimestampsRegionImpl implements TimestampsRegion {

    private static final Logger log = Logger.getLogger(TimestampsRegionImpl.class);

    private final InternalCache cache;
    private final String name;
    private final InternalRegionImpl internalRegion;
    private final RegionFactory regionFactory;

    TimestampsRegionImpl(InternalCache cache, String name, RegionFactory regionFactory) {
        this.cache = cache;
        this.name = name;
        this.regionFactory = regionFactory;
        this.internalRegion = new InternalRegionImpl(this);
    }

    @Override
    public Object getFromCache(Object key, SharedSessionContractImplementor session) {
        if (internalRegion.checkValid()) {
            return cache.getOrNull(key);
        }
        return null;
    }

    @Override
    public void putIntoCache(Object key, Object value, SharedSessionContractImplementor session) {
        cache.put(key, value);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RegionFactory getRegionFactory() {
        return regionFactory;
    }

    @Override
    public void clear() {
        internalRegion.beginInvalidation();
        runInvalidation();
        internalRegion.endInvalidation();
    }

    private void runInvalidation() {
        log.tracef("Non-transactional, clear in one go");
        cache.invalidateAll();
    }

    @Override
    public void destroy() {
    }

}
