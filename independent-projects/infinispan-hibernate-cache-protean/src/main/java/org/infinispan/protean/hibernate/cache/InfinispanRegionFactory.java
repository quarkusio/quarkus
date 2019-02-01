package org.infinispan.protean.hibernate.cache;

import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.*;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.support.RegionNameQualifier;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class InfinispanRegionFactory implements RegionFactory {

    private static final Logger log = Logger.getLogger(InfinispanRegionFactory.class);

    private static final String PREFIX = "hibernate.cache.";
    private static final String SIZE_SUFFIX = ".memory.size";
    private static final String MAX_IDLE_SUFFIX = ".expiration.max_idle";
    private static final String TEST_TIME_SERVICE = "test.hibernate.cache.time_service";

    private final Map<String, InternalCache> caches = new HashMap<>();

    private SessionFactoryOptions settings;
    private CacheKeysFactory cacheKeysFactory;

    private List<Region> regions = new ArrayList<>();
    private final Map<String, InternalCacheConfig> cacheConfigs = new HashMap<>();

    private TimeService timeService;

    public InfinispanRegionFactory() {
    }

    // Required by Hibernate
    @SuppressWarnings({"UnusedParameters", "unused"})
    public InfinispanRegionFactory(Properties props) {
        this();
    }

    @Override
    public void start(SessionFactoryOptions settings, Map configValues) {
        log.debug("Starting Infinispan region factory");
        // TODO: Customise this generated block

        // determine the CacheKeysFactory to use...
        this.cacheKeysFactory = determineCacheKeysFactory(settings, configValues);

        this.settings = settings;
        for (Object k : configValues.keySet()) {
            final String key = (String) k;
            int prefixIndex;
            if ((prefixIndex = key.indexOf(PREFIX)) != -1) {
                int prefixIndexEnd = prefixIndex + PREFIX.length();
                final String regionName = extractRegionName(prefixIndexEnd, key);
                if (regionName != null) {
                    cacheConfigs.compute(regionName, (ignore, cacheConfig) -> {
                        final String value = extractProperty(key, configValues);

                        if (cacheConfig == null)
                            cacheConfig = new InternalCacheConfig();

                        if (key.contains(SIZE_SUFFIX)) {
                            cacheConfig.setMaxSize(Long.parseLong(value));
                        } else if (key.contains(MAX_IDLE_SUFFIX)) {
                            cacheConfig.setMaxIdle(Duration.ofSeconds(Long.parseLong(value)));
                        }

                        return cacheConfig;
                    });
                }
            }
        }

        final Object testTimeService = configValues.get(TEST_TIME_SERVICE);
        if (testTimeService != null && testTimeService.equals("manual")) {
            timeService = new ManualTestService();
        } else {
            timeService = new CaffeineTimeService();
        }
    }

    private String extractRegionName(int prefixIndexEnd, String key) {
        final int suffixIndex = Math.max(key.indexOf(SIZE_SUFFIX), key.indexOf(MAX_IDLE_SUFFIX));
        if (suffixIndex != -1) {
            return key.substring(prefixIndexEnd, suffixIndex);
        }

        return null;
    }

    private String extractProperty(String key, Map properties) {
        final String value = ConfigurationHelper.extractPropertyValue(key, properties);
        log.debugf("Configuration override via property %s: %s", key, value);
        return value;
    }

    private CacheKeysFactory determineCacheKeysFactory(SessionFactoryOptions settings, Map properties) {
        return settings
                .getServiceRegistry()
                .getService(StrategySelector.class)
                .resolveDefaultableStrategy(
                        CacheKeysFactory.class,
                        properties.get(AvailableSettings.CACHE_KEYS_FACTORY),
                        DefaultCacheKeysFactory.INSTANCE
                );
    }

    @Override
    public boolean isMinimalPutsEnabledByDefault() {
        return false;
    }

    @Override
    public AccessType getDefaultAccessType() {
        return AccessType.READ_WRITE;
    }

    @Override
    public String qualify(String regionName) {
        return RegionNameQualifier.INSTANCE.qualify(regionName, settings);
    }

    @Override
    public CacheTransactionSynchronization createTransactionContext(SharedSessionContractImplementor session) {
        return new Sync(this);
    }

    @Override
    public long nextTimestamp() {
        return System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    public <T extends TimeService> T getTimeService() {
        return (T) timeService;
    }

    @Override
    public DomainDataRegion buildDomainDataRegion(DomainDataRegionConfig regionConfig, DomainDataRegionBuildingContext ctx) {
        log.debugf("Building domain data region [%s] entities=%s collections=%s naturalIds=%s",
                regionConfig.getRegionName(),
                regionConfig.getEntityCaching(),
                regionConfig.getCollectionCaching(),
                regionConfig.getNaturalIdCaching()
        );

        final String cacheName = qualify(regionConfig.getRegionName());
        InternalCache cache = getCache(cacheName);
        final DomainDataRegionImpl region = new DomainDataRegionImpl(cache, regionConfig, cacheKeysFactory, this);
        regions.add(region);
        return region;
    }

    private InternalCache getCache(String cacheName) {
        return caches.compute(cacheName, (ignore, cache) -> {
            if (cache == null) {
                final InternalCacheConfig cacheConfig = cacheConfigs.get(cacheName);
                cache = new CaffeineCache(cacheName, cacheConfig, timeService);
            }

            return cache;
        });
    }

    @Override
    public QueryResultsRegion buildQueryResultsRegion(String regionName, SessionFactoryImplementor sessionFactoryImplementor) {
        log.debugf("Building query results cache region [%s]", regionName);

        final String cacheName = qualify(regionName);
        InternalCache cache = getCache(cacheName);
        QueryResultsRegionImpl region = new QueryResultsRegionImpl(cache, regionName, this);
        regions.add(region);
        return region;
    }

    @Override
    public TimestampsRegion buildTimestampsRegion(String regionName, SessionFactoryImplementor sessionFactory) {
        log.debugf("Building timestamps cache region [%s]", regionName);

        final String cacheName = qualify(regionName);
        InternalCache cache = getCache(cacheName);
        TimestampsRegionImpl region = new TimestampsRegionImpl(cache, regionName, this);
        regions.add(region);
        return region;
    }

    @Override
    public void stop() {
        log.debug("Stop region factory");
        stopCacheRegions();
        stopCaches();
    }

    private void stopCaches() {
        caches.forEach((name, cache) -> {
            cache.stop();
        });
    }

    private void stopCacheRegions() {
        log.debug("Clear region references");

        // Ensure we cleanup any caches we created
        regions.forEach(Region::destroy);
        regions.clear();
    }

}
