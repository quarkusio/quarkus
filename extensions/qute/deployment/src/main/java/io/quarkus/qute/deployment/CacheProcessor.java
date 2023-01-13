package io.quarkus.qute.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.cache.deployment.spi.AdditionalCacheNameBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.qute.cache.QuteCache;

public class CacheProcessor {

    @BuildStep
    void initialize(Capabilities capabilities, BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<AdditionalCacheNameBuildItem> cacheNames) {
        if (capabilities.isPresent(Capability.CACHE)) {
            beans.produce(new AdditionalBeanBuildItem("io.quarkus.qute.runtime.cache.CacheConfigurator"));
            // We need to produce additional cache name because quarkus-cache only considers the CombinedIndexBuildItem and not the bean archive index
            cacheNames.produce(new AdditionalCacheNameBuildItem(QuteCache.NAME));
        }
    }

}
