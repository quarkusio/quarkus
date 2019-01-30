package org.infinispan.protean.hibernate.cache;

import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.CachedDomainDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

abstract class AbstractDomainDataAccess implements CachedDomainDataAccess {

    final InternalDataAccess internal;
    final DomainDataRegionImpl region;

    AbstractDomainDataAccess(InternalDataAccess internal, DomainDataRegionImpl region) {
        this.internal = internal;
        this.region = region;
    }

    @Override
    public DomainDataRegion getRegion() {
        return region;
    }

    @Override
    public Object get(SharedSessionContractImplementor session, Object key) {
        return internal.get(session, key, session.getTransactionStartTimestamp());
    }

    @Override
    public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, Object version) {
        return internal.putFromLoad(session, key, value, session.getTransactionStartTimestamp(), version);
    }

    @Override
    public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, Object version, boolean minimalPutOverride) {
        return internal.putFromLoad(session, key, value, session.getTransactionStartTimestamp(), version, minimalPutOverride);
    }

    @Override
    public SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version) {
        return null;
    }

    @Override
    public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) {
    }

    @Override
    public void remove(SharedSessionContractImplementor session, Object key) {
        internal.remove(session, key);
    }

    @Override
    public void removeAll(SharedSessionContractImplementor session) {
        internal.removeAll();
    }

    @Override
    public boolean contains(Object key) {
        return internal.get(null, key, Long.MAX_VALUE) != null;
    }

    @Override
    public SoftLock lockRegion() {
        return null;
    }

    @Override
    public void unlockRegion(SoftLock lock) {
    }

    @Override
    public void evict(Object key) {
        internal.evict(key);
    }

    @Override
    public void evictAll() {
        internal.evictAll();
    }

}
