package io.quarkus.cache.deployment.spi;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.cache.CacheManagerInfo;

/**
 * A build item that makes sure a {@link CacheManagerInfo} is available at runtime for consideration as the cache
 * backend
 */
public final class CacheManagerInfoBuildItem extends MultiBuildItem {

    private final CacheManagerInfo info;

    public CacheManagerInfoBuildItem(CacheManagerInfo info) {
        this.info = info;
    }

    public CacheManagerInfo get() {
        return info;
    }
}
