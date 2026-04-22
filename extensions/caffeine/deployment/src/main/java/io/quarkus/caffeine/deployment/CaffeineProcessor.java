package io.quarkus.caffeine.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.caffeine.runtime.graal.CacheConstructorsFeature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeRootClassBuildItem;
import io.quarkus.runtime.metrics.MetricsFactory;

public class CaffeineProcessor {

    private static final String CACHE_LOADER_CLASS_NAME = "com.github.benmanes.caffeine.cache.CacheLoader";

    /**
     * This specific implementation is always found, and doesn't need reflective registrations.
     */
    private static final String CACHE_LOADER_TO_EXCLUDE = "com.github.benmanes.caffeine.cache.BoundedLocalCache$BoundedLocalAsyncLoadingCache$AsyncLoader";

    private static final DotName CACHE_LOADER_NAME = DotName.createSimple(CACHE_LOADER_CLASS_NAME);

    @BuildStep
    void cacheLoaders(CombinedIndexBuildItem combinedIndex, BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        final Collection<ClassInfo> implementors = combinedIndex.getIndex().getAllKnownImplementors(CACHE_LOADER_NAME);
        List<String> effectiveImplementorNames = new ArrayList<>(implementors.size());
        for (ClassInfo info : implementors) {
            if (CACHE_LOADER_TO_EXCLUDE.equals(info.name().toString())) {
                continue;
            }
            effectiveImplementorNames.add(info.name().toString());
        }
        if (!effectiveImplementorNames.isEmpty()) {
            //Do not force registering any Caffeine classes if we can avoid it: there's a significant chain reaction
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(CACHE_LOADER_CLASS_NAME)
                    .reason(getClass().getName())
                    .methods().build());

            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(effectiveImplementorNames)
                    .reason(getClass().getName())
                    .methods().build());
        }
    }

    @BuildStep
    NativeImageFeatureBuildItem nativeImageFeature() {
        return new NativeImageFeatureBuildItem(CacheConstructorsFeature.class);
    }

    @BuildStep
    NativeImageSystemPropertyBuildItem registerRecordStatsImplementationsIfMicrometerAround(
            Optional<MetricsCapabilityBuildItem> metricsCapability) {
        if (metricsCapability.isEmpty()) {
            return null;
        }
        if (!metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)) {
            return null;
        }

        return new NativeImageSystemPropertyBuildItem(CacheConstructorsFeature.REGISTER_RECORD_STATS_IMPLEMENTATIONS,
                "true");
    }

    /**
     * Caffeine's {@code LocalCacheFactory} uses {@code MethodHandles.Lookup.findClass()}
     * to load cache implementation classes by dynamically constructed names (e.g. {@code SSMSA}).
     * Register the known variants as tree-shake roots so they survive JAR tree shaking.
     */
    @BuildStep
    void collectCaffeineTreeShakeRoots(BuildProducer<JarTreeShakeRootClassBuildItem> roots) {
        for (String className : CacheConstructorsFeature.typesNeedingConstructorsRegistered()) {
            roots.produce(new JarTreeShakeRootClassBuildItem(className));
        }
        for (String className : CacheConstructorsFeature.typesNeedingConstructorsRegisteredWhenRecordingStats()) {
            roots.produce(new JarTreeShakeRootClassBuildItem(className));
        }
    }
}
