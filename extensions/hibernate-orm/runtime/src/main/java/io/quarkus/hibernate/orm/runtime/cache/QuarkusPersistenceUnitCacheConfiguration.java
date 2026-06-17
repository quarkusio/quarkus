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

    public record Cache(long maxSize, Duration maxIdle) {
        public static Cache DEFAULT = new Cache(10000L, Duration.ofSeconds(100));
    }
}
