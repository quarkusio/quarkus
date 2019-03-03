package org.infinispan.quarkus.hibernate.cache;

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

public final class QuarkusInfinispanRegionFactory implements RegionFactory {

    private static final Logger log = Logger.getLogger(QuarkusInfinispanRegionFactory.class);

    private static final String PREFIX = "hibernate.cache.";
    private static final String OBJECT_COUNT_SUFFIX = ".memory.object-count";
    private static final String MAX_IDLE_SUFFIX = ".expiration.max-idle";

    private final Map<String, InternalCache> caches = new HashMap<>();

    private SessionFactoryOptions settings;
    private CacheKeysFactory cacheKeysFactory;

    private List<Region> regions = new ArrayList<>();
    private Map<String, InternalCacheConfig> cacheConfigs;

    private Time.MillisService regionTimeService;
    private Time.NanosService cacheTimeService;

    public QuarkusInfinispanRegionFactory() {
    }

    // Required by Hibernate
    @SuppressWarnings({"UnusedParameters", "unused"})
    public QuarkusInfinispanRegionFactory(Properties props) {
        this();
    }

    @Override
    public void start(SessionFactoryOptions settings, Map configValues) {
        log.debug("Starting Infinispan region factory");
        // TODO: Customise this generated block

        // determine the CacheKeysFactory to use...
        this.cacheKeysFactory = determineCacheKeysFactory(settings, configValues);

        if (regionTimeService == null)
            regionTimeService = Time.MillisService.SYSTEM;

        if (cacheTimeService == null)
            cacheTimeService = CaffeineCache.TIME_SERVICE;

        this.settings = settings;
        this.cacheConfigs = computeCacheConfigs(configValues);
    }

    private String extractRegionName(int prefixIndexEnd, String key) {
        final int suffixIndex = Math.max(key.indexOf(OBJECT_COUNT_SUFFIX), key.indexOf(MAX_IDLE_SUFFIX));
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
        return regionTimeService.milliTime();
    }

    void setRegionTimeService(Time.MillisService regionTimeService) {
        this.regionTimeService = regionTimeService;
    }

    void setCacheTimeService(Time.NanosService cacheTimeService) {
        this.cacheTimeService = cacheTimeService;
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
                final InternalCacheConfig userDefinedCacheConfig = cacheConfigs.get(cacheName);
                final InternalCacheConfig cacheConfig = userDefinedCacheConfig == null ? defaultDomainCacheConfig() : userDefinedCacheConfig;
                cache = new CaffeineCache(cacheName, cacheConfig, this.cacheTimeService);
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

    private HashMap<String, InternalCacheConfig> computeCacheConfigs(Map configValues) {
        final HashMap<String, InternalCacheConfig> cacheConfigs = new HashMap<>();

        cacheConfigs.put(DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME, defaultQueryCacheConfig());
        cacheConfigs.put(DEFAULT_UPDATE_TIMESTAMPS_REGION_UNQUALIFIED_NAME, defaultTimestampsCacheConfig());

        for (Object k : configValues.keySet()) {
            final String key = (String) k;
            int prefixIndex;
            if ((prefixIndex = key.indexOf(PREFIX)) != -1) {
                int prefixIndexEnd = prefixIndex + PREFIX.length();
                final String regionName = extractRegionName(prefixIndexEnd, key);
                if (regionName != null) {
                    cacheConfigs.compute(regionName, (ignore, cacheConfig) -> {
                        final String value = extractProperty(key, configValues);

                        if (cacheConfig == null) {
                            // Query and timestamps regions already defined, so it can only be a domain region
                            cacheConfig = defaultDomainCacheConfig();
                        }

                        if (key.contains(OBJECT_COUNT_SUFFIX)) {
                            cacheConfig.objectCount = Long.parseLong(value);
                        } else if (key.contains(MAX_IDLE_SUFFIX)) {
                            cacheConfig.maxIdle = Duration.ofSeconds(Long.parseLong(value));
                        }

                        return cacheConfig;
                    });
                }
            }
        }
        return cacheConfigs;
    }

    private static InternalCacheConfig defaultQueryCacheConfig() {
        // Query results follows same defaults as entity/collections.
        // That is, sensible defaults to avoid running out of memory.
        InternalCacheConfig cacheConfig = new InternalCacheConfig();
        cacheConfig.maxIdle = Duration.ofSeconds(100);
        cacheConfig.objectCount = 10_000;
        return cacheConfig;
    }

    private static InternalCacheConfig defaultTimestampsCacheConfig() {
        // Update timestamps should not be automatically evicted.
        // They're required to verify whether a query can be served from cache.
        InternalCacheConfig cacheConfig = new InternalCacheConfig();
        cacheConfig.maxIdle = Time.forever();
        cacheConfig.objectCount = -1;
        return cacheConfig;
    }

    private static InternalCacheConfig defaultDomainCacheConfig() {
        // Sensible defaults to avoid running out of memory
        InternalCacheConfig cacheConfig = new InternalCacheConfig();
        cacheConfig.maxIdle = Duration.ofSeconds(100);
        cacheConfig.objectCount = 10_000;
        return cacheConfig;
    }

    public Optional<Long> getMemoryObjectCount(String region) {
        InternalCacheConfig config = cacheConfigs.get(region);
        return config == null ? Optional.empty() : Optional.of(config.objectCount);
    }

    public Optional<Duration> getExpirationMaxIdle(String region) {
        InternalCacheConfig config = cacheConfigs.get(region);
        return config == null ? Optional.empty() : Optional.of(config.maxIdle);
    }

}
