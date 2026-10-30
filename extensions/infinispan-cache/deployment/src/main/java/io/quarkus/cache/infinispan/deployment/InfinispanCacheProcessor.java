package io.quarkus.cache.infinispan.deployment;

import org.infinispan.client.hotrod.RemoteCacheManager;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.cache.CacheManagerInfo;
import io.quarkus.cache.CompositeCacheKey;
import io.quarkus.cache.infinispan.runtime.CompositeKeyMarshallerBean;
import io.quarkus.cache.infinispan.runtime.InfinispanCacheBuildRecorder;
import io.quarkus.cache.infinispan.runtime.InfinispanCachesBuildTimeConfig;
import io.quarkus.cache.infinispan.runtime.InfinispanCachesConfig;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.infinispan.client.deployment.InfinispanClientNameBuildItem;
import io.quarkus.infinispan.client.runtime.InfinispanClientUtil;

public class InfinispanCacheProcessor {

    private static final String INFINISPAN_CACHE_TYPE = "infinispan";

    @BuildStep
    void cacheManagerInfo(ServiceRegistrar serviceRegistrar, InfinispanCachesBuildTimeConfig buildConfig) {
        serviceRegistrar.forService(CacheManagerInfo.class, INFINISPAN_CACHE_TYPE)
                .require(InfinispanCachesConfig.class)
                .onStart((ctx, runtimeConfig) -> InfinispanCacheBuildRecorder.getCacheManagerSupplier(buildConfig,
                        runtimeConfig));
    }

    @BuildStep
    void ensureAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(CompositeKeyMarshallerBean.class));
    }

    @BuildStep
    UnremovableBeanBuildItem ensureBeanLookupAvailable() {
        return UnremovableBeanBuildItem.beanTypes(RemoteCacheManager.class);
    }

    @BuildStep
    InfinispanClientNameBuildItem requestedInfinispanClientBuildItem(InfinispanCachesBuildTimeConfig buildConfig) {
        return new InfinispanClientNameBuildItem(
                buildConfig.clientName().orElse(InfinispanClientUtil.DEFAULT_INFINISPAN_CLIENT_NAME));
    }

    @BuildStep
    void nativeImage(BuildProducer<ReflectiveClassBuildItem> producer) {
        producer.produce(ReflectiveClassBuildItem.builder(CompositeCacheKey.class)
                .reason(getClass().getName())
                .methods(true).build());
    }

}
