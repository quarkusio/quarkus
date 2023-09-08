package io.quarkus.qute.deployment;

import java.util.Optional;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.cache.deployment.spi.AdditionalCacheNameBuildItem;
import io.quarkus.cache.deployment.spi.CacheTypeBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.qute.cache.QuteCache;

public class CacheProcessor {

    @BuildStep
    void initialize(Optional<CacheTypeBuildItem> cacheTypeBuildItem,
            BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<AdditionalCacheNameBuildItem> cacheNames) {
        if (cacheTypeBuildItem.isEmpty()) { // no caching enabled
            return;
        }
        CacheTypeBuildItem.Type type = cacheTypeBuildItem.get().getType();
        if (type != CacheTypeBuildItem.Type.LOCAL) { // it does not make sense to use a remote cache for Qute
            return;
        }
        beans.produce(new AdditionalBeanBuildItem("io.quarkus.qute.runtime.cache.CacheConfigurator"));
        // We need to produce additional cache name because quarkus-cache only considers the CombinedIndexBuildItem and not the bean archive index
        cacheNames.produce(new AdditionalCacheNameBuildItem(QuteCache.NAME));
    }

}
