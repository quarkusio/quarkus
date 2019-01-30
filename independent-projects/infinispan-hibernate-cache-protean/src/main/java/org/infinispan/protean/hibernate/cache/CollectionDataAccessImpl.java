package org.infinispan.protean.hibernate.cache;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;

final class CollectionDataAccessImpl extends AbstractDomainDataAccess implements CollectionDataAccess {

    private final AccessType accessType;

    CollectionDataAccessImpl(AccessType accessType, InternalDataAccess internal, DomainDataRegionImpl region) {
        super(internal, region);
        this.accessType = accessType;
    }

    @Override
    public Object generateCacheKey(Object id, CollectionPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
        return region.getCacheKeysFactory().createCollectionKey(id, persister, factory, tenantIdentifier);
    }

    @Override
    public Object getCacheKeyId(Object cacheKey) {
        return region.getCacheKeysFactory().getCollectionId(cacheKey);
    }

    @Override
    public AccessType getAccessType() {
        return accessType;
    }

}
