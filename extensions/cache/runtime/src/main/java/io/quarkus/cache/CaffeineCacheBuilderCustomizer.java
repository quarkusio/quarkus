package io.quarkus.cache;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Meant to be implemented by CDI beans that provides arbitrary customization for the {@link Caffeine} builder that get created by Quarkus based on user configuration.
 * <p>
 * If an implementation is annotated with {@link CacheName} then the customizer only applies to that cache, otherwise if no such qualifier is present,
 * then the customizer is applied to all caches.
 */
public interface CaffeineCacheBuilderCustomizer extends Comparable<CaffeineCacheBuilderCustomizer> {

    int MINIMUM_PRIORITY = Integer.MIN_VALUE;

    int DEFAULT_PRIORITY = 0;

    void customize(Caffeine<Object, Object> cacheBuilder);

    /**
     * Defines the priority that the customizers are applied.
     * A lower integer value means that the customizer will be applied after a customizer with a higher priority
     */
    default int priority() {
        return DEFAULT_PRIORITY;
    }

    default int compareTo(CaffeineCacheBuilderCustomizer o) {
        return Integer.compare(o.priority(), priority());
    }
}
