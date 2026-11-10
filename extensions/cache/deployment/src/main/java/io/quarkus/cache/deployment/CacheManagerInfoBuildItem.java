package io.quarkus.cache.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.cache.CacheManagerInfo;

/**
 * A build item that makes sure a {@link CacheManagerInfo} is available at runtime for consideration as the cache backend
 *
 * @deprecated register a {@link CacheManagerInfo} service with the {@code ServiceRegistrar} instead, named after the cache
 *             type; the cache manager consumes all services of that type
 */
@Deprecated(since = "4.0", forRemoval = true)
public final class CacheManagerInfoBuildItem extends MultiBuildItem {

    private final CacheManagerInfo info;

    public CacheManagerInfoBuildItem(CacheManagerInfo info) {
        this.info = info;
    }

    public CacheManagerInfo get() {
        return info;
    }
}
