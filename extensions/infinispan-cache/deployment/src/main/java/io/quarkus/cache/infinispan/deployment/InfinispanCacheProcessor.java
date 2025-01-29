package io.quarkus.cache.infinispan.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import org.infinispan.client.hotrod.RemoteCacheManager;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.cache.CompositeCacheKey;
import io.quarkus.cache.deployment.spi.CacheManagerInfoBuildItem;
import io.quarkus.cache.infinispan.runtime.CompositeKeyMarshallerBean;
import io.quarkus.cache.infinispan.runtime.InfinispanCacheBuildRecorder;
import io.quarkus.cache.infinispan.runtime.InfinispanCachesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.infinispan.client.deployment.InfinispanClientNameBuildItem;
import io.quarkus.infinispan.client.runtime.InfinispanClientUtil;

public class InfinispanCacheProcessor {

    @BuildStep
    @Record(RUNTIME_INIT)
    CacheManagerInfoBuildItem cacheManagerInfo(BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            InfinispanCacheBuildRecorder recorder) {
        return new CacheManagerInfoBuildItem(recorder.getCacheManagerSupplier());
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
