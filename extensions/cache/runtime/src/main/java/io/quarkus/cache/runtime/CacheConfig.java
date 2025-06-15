package io.quarkus.cache.runtime;

import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigRoot(phase = RUN_TIME)
@ConfigMapping(prefix = "quarkus.cache")
public interface CacheConfig {

    /**
     * Whether or not the cache extension is enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Caffeine configuration.
     */
    CaffeineConfig caffeine();

    interface CaffeineConfig {

        /**
         * Default configuration applied to all Caffeine caches (lowest precedence)
         */
        @WithParentName
        @ConfigDocSection
        CaffeineCacheConfig defaultConfig();

        /**
         * Additional configuration applied to a specific Caffeine cache (highest precedence)
         */
        @WithParentName
        @ConfigDocMapKey("cache-name")
        @ConfigDocSection
        Map<String, CaffeineCacheConfig> cachesConfig();

        interface CaffeineCacheConfig {

            /**
             * Minimum total size for the internal data structures. Providing a large enough estimate at construction
             * time avoids the need for expensive resizing operations later, but setting this value unnecessarily high
             * wastes memory.
             */
            OptionalInt initialCapacity();

            /**
             * Maximum number of entries the cache may contain. Note that the cache <b>may evict an entry before this
             * limit is exceeded or temporarily exceed the threshold while evicting</b>. As the cache size grows close
             * to the maximum, the cache evicts entries that are less likely to be used again. For example, the cache
             * may evict an entry because it hasn't been used recently or very often.
             */
            OptionalLong maximumSize();

            /**
             * Specifies that each entry should be automatically removed from the cache once a fixed duration has
             * elapsed after the entry's creation, or the most recent replacement of its value.
             */
            Optional<Duration> expireAfterWrite();

            /**
             * Specifies that each entry should be automatically removed from the cache once a fixed duration has
             * elapsed after the entry's creation, the most recent replacement of its value, or its last read.
             */
            Optional<Duration> expireAfterAccess();

            /**
             * Whether or not metrics are recorded if the application depends on the Micrometer extension. Setting this
             * value to {@code true} will enable the accumulation of cache stats inside Caffeine.
             */
            Optional<Boolean> metricsEnabled();
        }
    }
}
