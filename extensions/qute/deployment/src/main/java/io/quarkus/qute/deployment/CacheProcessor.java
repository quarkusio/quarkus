package io.quarkus.qute.deployment;

import java.util.Optional;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.cache.deployment.spi.AdditionalCacheNameBuildItem;
import io.quarkus.cache.deployment.spi.CacheTypeBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.qute.cache.QuteCache;
import io.quarkus.qute.runtime.cache.CacheConfigurator;
import io.quarkus.qute.runtime.cache.MissingCacheConfigurator;
import io.quarkus.qute.runtime.cache.UnsupportedRemoteCacheConfigurator;

public class CacheProcessor {

    @BuildStep
    void initialize(Optional<CacheTypeBuildItem> cacheTypeBuildItem, BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<AdditionalCacheNameBuildItem> cacheNames) {
        Class configuratorClass;
        boolean supported = false;
        if (cacheTypeBuildItem.isEmpty()) { // no caching enabled
            configuratorClass = MissingCacheConfigurator.class;
        } else {
            CacheTypeBuildItem.Type type = cacheTypeBuildItem.get().getType();
            if (type != CacheTypeBuildItem.Type.LOCAL) { // it does not make sense to use a remote cache for Qute
                configuratorClass = UnsupportedRemoteCacheConfigurator.class;
            } else {
                configuratorClass = CacheConfigurator.class;
                supported = true;
            }
        }

        beans.produce(new AdditionalBeanBuildItem(configuratorClass.getName()));
        // We need to produce additional cache name because quarkus-cache only considers the CombinedIndexBuildItem and
        // not the bean archive index
        if (supported) {
            cacheNames.produce(new AdditionalCacheNameBuildItem(QuteCache.NAME));
        }
    }

}
