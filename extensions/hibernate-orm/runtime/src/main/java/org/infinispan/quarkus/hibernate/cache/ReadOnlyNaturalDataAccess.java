package org.infinispan.quarkus.hibernate.cache;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

final class ReadOnlyNaturalDataAccess extends AbstractDomainDataAccess implements NaturalIdDataAccess {

    ReadOnlyNaturalDataAccess(InternalDataAccess internal, DomainDataRegionImpl region) {
        super(internal, region);
    }

    @Override
    public AccessType getAccessType() {
        return AccessType.READ_ONLY;
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
        throw new UnsupportedOperationException("Illegal attempt to edit read only item");
    }

    @Override
    public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, SoftLock lock) {
        throw new UnsupportedOperationException("Illegal attempt to edit read only item");
    }

}
