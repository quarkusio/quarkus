package org.infinispan.protean.hibernate.cache;

import org.hibernate.cache.cfg.spi.*;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.ExtendedStatisticsSupport;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.stat.CacheRegionStatistics;
import org.jboss.logging.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class DomainDataRegionImpl implements DomainDataRegion, ExtendedStatisticsSupport {

    private static final Logger log = Logger.getLogger(DomainDataRegionImpl.class);

    private final InternalCache cache;
    private final DomainDataRegionConfig config;
    private final CacheKeysFactory cacheKeysFactory;
    private final RegionFactory regionFactory;
    private final InternalRegion internalRegion;

    private Strategy strategy;
    private PutFromLoadValidator validator;
    private Predicate<Map.Entry> filter;

    @Override
    public long getElementCountInMemory() {
        return cache.size(filter);
    }

    @Override
    public long getElementCountOnDisk() {
        return CacheRegionStatistics.NO_EXTENDED_STAT_SUPPORT_RETURN;
    }

    @Override
    public long getSizeInMemory() {
        return CacheRegionStatistics.NO_EXTENDED_STAT_SUPPORT_RETURN;
    }

    private enum Strategy {
        VALIDATION, VERSIONED_ENTRIES
    }

    DomainDataRegionImpl(InternalCache cache, DomainDataRegionConfig config, CacheKeysFactory cacheKeysFactory, RegionFactory regionFactory) {
        this.cache = cache;
        this.config = config;
        this.cacheKeysFactory = cacheKeysFactory;
        this.regionFactory = regionFactory;
        this.internalRegion = new InternalRegionImpl(this);
    }

    CacheKeysFactory getCacheKeysFactory() {
        return cacheKeysFactory;
    }

    @Override
    public EntityDataAccess getEntityDataAccess(NavigableRole rootEntityRole) {
        EntityDataCachingConfig entityConfig = findConfig(config.getEntityCaching(), rootEntityRole);
        AccessType accessType = entityConfig.getAccessType();

        Comparator<Object> comparator =
                entityConfig.isVersioned() ? entityConfig.getVersionComparatorAccess().get() : null;
        InternalDataAccess internal = createInternalDataAccess(accessType, comparator);

        if (accessType == AccessType.READ_ONLY || !entityConfig.isMutable())
            return new ReadOnlyEntityDataAccess(internal, this);

        return new ReadWriteEntityDataAccess(internal, this);
    }

    private <T extends DomainDataCachingConfig> T findConfig(List<T> configs, NavigableRole role) {
        return configs.stream()
                .filter(c -> c.getNavigableRole().equals(role))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cannot find configuration for " + role));
    }

    private synchronized InternalDataAccess createInternalDataAccess(
            AccessType accessType, Comparator<Object> comparator) {
        if (accessType == AccessType.NONSTRICT_READ_WRITE) {
            prepareForVersionedEntries();
            return new NonStrictDataAccess(cache, internalRegion, comparator, regionFactory);
        } else {
            prepareForValidation();
            return new StrictDataAccess(cache, validator, internalRegion);
        }
    }

    private void prepareForValidation() {
        if (strategy != null) {
            assert strategy == Strategy.VALIDATION;
            return;
        }
        validator = new PutFromLoadValidator(cache, config.getRegionName(), regionFactory);
        strategy = Strategy.VALIDATION;
    }

    private void prepareForVersionedEntries() {
        if (strategy != null) {
            assert strategy == Strategy.VERSIONED_ENTRIES;
            return;
        }

        filter = VersionedEntry.EXCLUDE_EMPTY_VERSIONED_ENTRY;

        // TODO why do this each time data access is requested? config is set on constructor...
        for (EntityDataCachingConfig entityConfig : config.getEntityCaching()) {
            if (entityConfig.isVersioned()) {
                for (NavigableRole role : entityConfig.getCachedTypes()) {
                    internalRegion.addComparator(role.getNavigableName(), entityConfig.getVersionComparatorAccess().get());
                }
            }
        }
        for (CollectionDataCachingConfig collectionConfig : config.getCollectionCaching()) {
            if (collectionConfig.isVersioned()) {
                internalRegion.addComparator(collectionConfig.getNavigableRole().getNavigableName(), collectionConfig.getOwnerVersionComparator());
            }
        }

        strategy = Strategy.VERSIONED_ENTRIES;
    }

    @Override
    public NaturalIdDataAccess getNaturalIdDataAccess(NavigableRole rootEntityRole) {
        NaturalIdDataCachingConfig naturalIdConfig = findConfig(this.config.getNaturalIdCaching(), rootEntityRole);
        AccessType accessType = naturalIdConfig.getAccessType();
        if (accessType == AccessType.NONSTRICT_READ_WRITE) {
            // We don't support nonstrict read write for natural ids as NSRW requires versions;
            // natural ids aren't versioned by definition (as the values are primary keys).
            accessType = AccessType.READ_WRITE;
        }

        InternalDataAccess internal = createInternalDataAccess(accessType, null);
        if (accessType == AccessType.READ_ONLY || !naturalIdConfig.isMutable()) {
            return new ReadOnlyNaturalDataAccess(internal, this);
        } else {
            return new ReadWriteNaturalDataAccess(internal, this);
        }
    }

    @Override
    public CollectionDataAccess getCollectionDataAccess(NavigableRole collectionRole) {
        CollectionDataCachingConfig collectionConfig = findConfig(this.config.getCollectionCaching(), collectionRole);
        AccessType accessType = collectionConfig.getAccessType();
        InternalDataAccess accessDelegate = createInternalDataAccess(accessType, collectionConfig.getOwnerVersionComparator());
        // No update/afterUpdate in CollectionDataAccess so a single impl works for all access types
        return new CollectionDataAccessImpl(accessType, accessDelegate, this);
    }

    @Override
    public String getName() {
        return config.getRegionName();
    }

    @Override
    public RegionFactory getRegionFactory() {
        return regionFactory;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void clear() {
        internalRegion.beginInvalidation();
        runInvalidation();
        internalRegion.endInvalidation();
    }

    private void runInvalidation() {
        if (strategy == null) {
            throw new IllegalStateException("Strategy was not set");
        }
        switch (strategy) {
            case VALIDATION:
                log.tracef("Non-transactional, clear in one go");
                cache.invalidateAll();
                break;
            case VERSIONED_ENTRIES:
                // no need to use this as a function - simply override all
                VersionedEntry evict = new VersionedEntry(regionFactory.nextTimestamp(), VersionedEntry.TOMBSTONE_LIFESPAN);
                removeEntries(entry -> cache.put(entry.getKey(), evict));
                break;
        }
    }

    private void removeEntries(Consumer<Map.Entry> remover) {
        // We can never use cache.clear() since tombstones must be kept.
        cache.forEach(filter, remover);
    }

}
