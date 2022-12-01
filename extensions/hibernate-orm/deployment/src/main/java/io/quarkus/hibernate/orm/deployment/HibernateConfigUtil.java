package io.quarkus.hibernate.orm.deployment;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit.HibernateOrmConfigPersistenceUnitCache;

public class HibernateConfigUtil {

    /**
     * TODO: reuse the ones from QuarkusInfinispanRegionFactory as soon as they are made public.
     */
    private final static String EXPIRATION_MAX_IDLE = ".expiration.max-idle";
    private final static String MEMORY_OBJECT_COUNT = ".memory.object-count";
    private static final String HIBERNATE_CACHE_PREFIX = "hibernate.cache.";

    public static Map<String, String> getCacheConfigEntries(HibernateOrmConfigPersistenceUnit config) {
        Map<String, String> cacheRegionsConfigEntries = new HashMap<>();
        for (Map.Entry<String, HibernateOrmConfigPersistenceUnitCache> regionEntry : config.cache.entrySet()) {
            String regionName = regionEntry.getKey();
            HibernateOrmConfigPersistenceUnitCache cacheConfig = regionEntry.getValue();

            if (cacheConfig.expiration.maxIdle.isPresent()) {
                cacheRegionsConfigEntries.put(getCacheConfigKey(regionName, EXPIRATION_MAX_IDLE),
                        String.valueOf(cacheConfig.expiration.maxIdle.get().getSeconds()));
            }
            if (cacheConfig.memory.objectCount.isPresent()) {
                cacheRegionsConfigEntries.put(getCacheConfigKey(regionName, MEMORY_OBJECT_COUNT),
                        String.valueOf(cacheConfig.memory.objectCount.getAsLong()));
            }
        }

        return cacheRegionsConfigEntries;
    }

    private static String getCacheConfigKey(String regionName, String configKey) {
        return HIBERNATE_CACHE_PREFIX + regionName + configKey;
    }

    public static <T> OptionalInt firstPresent(OptionalInt first, OptionalInt second) {
        return first.isPresent() ? first : second;
    }
}
