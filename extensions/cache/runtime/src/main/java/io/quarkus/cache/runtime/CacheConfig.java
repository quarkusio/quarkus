package io.quarkus.cache.runtime;

import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = RUN_TIME)
public class CacheConfig {

    public static final String CAFFEINE_CACHE_TYPE = "caffeine";

    /**
     * Whether or not the cache extension is enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Cache type.
     */
    @ConfigItem(defaultValue = CAFFEINE_CACHE_TYPE)
    public String type;

    /**
     * Caffeine configuration.
     */
    @ConfigItem
    public CaffeineConfig caffeine;

    @ConfigGroup
    public static class CaffeineConfig {

        /**
         * Namespace configuration.
         */
        @ConfigItem(name = ConfigItem.PARENT)
        @ConfigDocMapKey("cache-name")
        public Map<String, CaffeineNamespaceConfig> namespace;

        @ConfigGroup
        public static class CaffeineNamespaceConfig {

            /**
             * Minimum total size for the internal data structures. Providing a large enough estimate at construction time
             * avoids the need for expensive resizing operations later, but setting this value unnecessarily high wastes memory.
             */
            @ConfigItem
            public OptionalInt initialCapacity;

            /**
             * Maximum number of entries the cache may contain. Note that the cache <b>may evict an entry before this limit is
             * exceeded or temporarily exceed the threshold while evicting</b>. As the cache size grows close to the maximum,
             * the cache evicts entries that are less likely to be used again. For example, the cache may evict an entry because
             * it hasn't been used recently or very often.
             */
            @ConfigItem
            public OptionalLong maximumSize;

            /**
             * Specifies that each entry should be automatically removed from the cache once a fixed duration has elapsed after
             * the entry's creation, or the most recent replacement of its value.
             */
            @ConfigItem
            public Optional<Duration> expireAfterWrite;

            /**
             * Specifies that each entry should be automatically removed from the cache once a fixed duration has elapsed after
             * the entry's creation, the most recent replacement of its value, or its last read.
             */
            @ConfigItem
            public Optional<Duration> expireAfterAccess;

            /**
             * Whether or not metrics are recorded if the application depends on the Micrometer extension. Setting this
             * value to {@code true} will enable the accumulation of cache stats inside Caffeine.
             */
            @ConfigItem
            public boolean metricsEnabled;
        }
    }
}
