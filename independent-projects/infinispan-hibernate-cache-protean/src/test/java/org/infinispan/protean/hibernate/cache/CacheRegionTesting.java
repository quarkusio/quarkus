package org.infinispan.protean.hibernate.cache;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.cfg.internal.DomainDataRegionConfigImpl;
import org.hibernate.cache.cfg.spi.DomainDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.ExtendedStatisticsSupport;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.type.VersionType;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class CacheRegionTesting {

    final SharedSessionContractImplementor session;
    private final DomainDataRegion region;
    private final DomainDataRegionConfig config;
    final ManualMillisService regionTimeService;
    final ManualNanosService cacheTimeService;

    private CacheRegionTesting(
            SharedSessionContractImplementor session,
            DomainDataRegion region,
            DomainDataRegionConfig config,
            ManualMillisService regionTimeService,
            ManualNanosService cacheTimeService) {
        this.session = session;
        this.region = region;
        this.config = config;
        this.regionTimeService = regionTimeService;
        this.cacheTimeService = cacheTimeService;
    }

    @SuppressWarnings("unchecked")
    <T extends DomainDataRegion & ExtendedStatisticsSupport> T region() {
        return (T) region;
    }

    EntityDataAccess entityCache(AccessType accessType) {
        final NavigableRole role = config.getEntityCaching().stream()
                .filter(c -> c.getAccessType() == accessType)
                .map(DomainDataCachingConfig::getNavigableRole)
                .findFirst().orElseThrow(IllegalArgumentException::new);

        return region.getEntityDataAccess(role);
    }

    static CacheRegionTesting cacheRegion(String regionName, Map configValues) {
        ProteanInfinispanRegionFactory regionFactory = new ProteanInfinispanRegionFactory();
        ManualMillisService regionTimeService = new ManualMillisService();
        regionFactory.setRegionTimeService(regionTimeService);
        ManualNanosService cacheTimeService = new ManualNanosService();
        regionFactory.setCacheTimeService(cacheTimeService);

        SessionFactoryOptions sessionFactoryOptions = sessionFactoryOptionsMock();
        regionFactory.start(sessionFactoryOptions, configValues);

        RootClass persistentClass = rootClassMock(regionName);
        DomainDataRegionConfig config = new DomainDataRegionConfigImpl.Builder(regionName)
                .addEntityConfig(persistentClass, AccessType.READ_WRITE)
                .build();
        DomainDataRegion region = regionFactory.buildDomainDataRegion(config, null);

        SharedSessionContractImplementor session = mock(SharedSessionContractImplementor.class);
        when(session.getTransactionStartTimestamp()).thenAnswer(x -> regionFactory.nextTimestamp());

        return new CacheRegionTesting(session, region, config, regionTimeService, cacheTimeService);
    }

    private static SessionFactoryOptions sessionFactoryOptionsMock() {
        StrategySelector strategySelector = mock(StrategySelector.class);
        StandardServiceRegistry registry = mock(StandardServiceRegistry.class);
        when(registry.getService(StrategySelector.class)).thenReturn(strategySelector);
        SessionFactoryOptions sessionFactoryOptions = mock(SessionFactoryOptions.class);
        when(sessionFactoryOptions.getServiceRegistry()).thenReturn(registry);
        return sessionFactoryOptions;
    }

    private static RootClass rootClassMock(String entityName) {
        RootClass persistentClass = mock(RootClass.class);
        when(persistentClass.getRootClass()).thenReturn(persistentClass);
        when(persistentClass.getEntityName()).thenReturn(entityName);
        Property versionMock = mock(Property.class);
        VersionType typeMock = mock(VersionType.class);
        when(typeMock.getComparator()).thenReturn(UNIVERSAL_COMPARATOR);
        when(versionMock.getType()).thenReturn(typeMock);
        when(persistentClass.getVersion()).thenReturn(versionMock);
        when(persistentClass.isVersioned()).thenReturn(true);
        when(persistentClass.isMutable()).thenReturn(true);
        return persistentClass;
    }

    private static final Comparator<Object> UNIVERSAL_COMPARATOR = (o1, o2) -> {
        if (o1 instanceof Long && o2 instanceof Long) {
            return ((Long) o1).compareTo((Long) o2);
        } else if (o1 instanceof Integer && o2 instanceof Integer) {
            return ((Integer) o1).compareTo((Integer) o2);
        }
        throw new UnsupportedOperationException();
    };

    static final class ManualMillisService implements Time.MillisService {
        private final AtomicLong millis = new AtomicLong();

        public void advance(long time, TimeUnit timeUnit) {
            final long milliseconds = timeUnit.toMillis(time);
            millis.addAndGet(milliseconds);
        }

        @Override
        public long milliTime() {
            return millis.get();
        }
    }

    static final class ManualNanosService implements Time.NanosService {
        private final AtomicLong nanos = new AtomicLong();

        public void advance(long time, TimeUnit timeUnit) {
            final long nanoseconds = timeUnit.toNanos(time);
            nanos.addAndGet(nanoseconds);
        }

        @Override
        public long nanoTime() {
            return nanos.get();
        }
    }

}
