package org.infinispan.protean.hibernate.cache;

import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

final class ReadWriteNaturalDataAccess extends AbstractDomainDataAccess implements NaturalIdDataAccess {

    ReadWriteNaturalDataAccess(InternalDataAccess internal, DomainDataRegionImpl region) {
        super(internal, region);
    }

    @Override
    public AccessType getAccessType() {
        return AccessType.READ_WRITE;
    }

    @Override
    public Object generateCacheKey(Object[] naturalIdValues, EntityPersister persister, SharedSessionContractImplementor session) {
        return region.getCacheKeysFactory().createNaturalIdKey(naturalIdValues, persister, session);
    }

    @Override
    public Object[] getNaturalIdValues(Object cacheKey) {
        return region.getCacheKeysFactory().getNaturalIdValues(cacheKey);
    }

    @Override
    public boolean insert(SharedSessionContractImplementor session, Object key, Object value) {
        return internal.insert(session, key, value, null);
    }

    @Override
    public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value) {
        return internal.afterInsert(session, key, value, null);
    }

    @Override
    public boolean update(SharedSessionContractImplementor session, Object key, Object value) {
        return internal.update(session, key, value, null, null);
    }

    @Override
    public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, SoftLock lock) {
        return internal.afterUpdate(session, key, value, null, null, lock);
    }

}
