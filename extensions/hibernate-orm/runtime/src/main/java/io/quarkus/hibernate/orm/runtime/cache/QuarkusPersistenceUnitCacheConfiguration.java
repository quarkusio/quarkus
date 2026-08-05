package io.quarkus.hibernate.orm.runtime.cache;

import java.time.Duration;
import java.util.Map;

/**
 * Cache configuration for a Hibernate persistence unit.
 * Created at build time and serialized into bytecode via recorders, used at runtime to configure Caffeine caches.
 */
public record QuarkusPersistenceUnitCacheConfiguration(Map<String, Cache> caches) {
    /**
     * The config key where this configuration must be placed in Hibernate properties.
     */
    public static final String CONFIG_KEY = "hibernate.cache.quarkus_caffeine_config";

    /**
     * @param maxSize maximum number of entries (count-based eviction). Ignored if maximumWeight is set.
     * @param maxIdle maximum idle time before expiration.
     * @param maximumWeight maximum total weight (weight-based eviction). {@code -1} means not set.
     * @param weigherClassName fully qualified class name of a Weigher implementation. {@code null} means not set.
     */
    public record Cache(long maxSize, Duration maxIdle, long maximumWeight, String weigherClassName) {

        public static Cache DEFAULT = new Cache(10000L, Duration.ofSeconds(100), -1L, null);

        public boolean hasMaximumWeight() {
            return maximumWeight >= 0;
        }

        public boolean hasWeigherClass() {
            return weigherClassName != null;
        }
    }
}
