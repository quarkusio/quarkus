package org.infinispan.protean.hibernate.cache;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

final class ReadWriteEntityDataAccess extends AbstractDomainDataAccess implements EntityDataAccess {

    ReadWriteEntityDataAccess(InternalDataAccess internal, DomainDataRegionImpl region) {
        super(internal, region);
    }

    @Override
    public Object generateCacheKey(Object id, EntityPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
        return region.getCacheKeysFactory().createEntityKey(id, persister, factory, tenantIdentifier);
    }

    @Override
    public Object getCacheKeyId(Object cacheKey) {
        return region.getCacheKeysFactory().getEntityId(cacheKey);
    }

    @Override
    public boolean insert(SharedSessionContractImplementor session, Object key, Object value, Object version) {
        return internal.insert(session, key, value, version);
    }

    @Override
    public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) {
        return internal.afterInsert(session, key, value, version);
    }

    @Override
    public boolean update(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion) {
        return internal.update(session, key, value, currentVersion, previousVersion);
    }

    @Override
    public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock) {
        return internal.afterUpdate(session, key, value, currentVersion, previousVersion, lock);
    }

    @Override
    public AccessType getAccessType() {
        return AccessType.READ_WRITE;
    }

}
