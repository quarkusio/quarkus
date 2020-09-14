package io.quarkus.caffeine.deployment;

import java.io.IOException;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class CaffeineProcessor {
    private static final String CACHE_LOADER_CLASS_NAME = "com.github.benmanes.caffeine.cache.CacheLoader";
    private static final DotName CACHE_LOADER_NAME = DotName.createSimple(CACHE_LOADER_CLASS_NAME);

    @BuildStep
    ReflectiveClassBuildItem cacheClasses() throws IOException {
        //todo: how to we want to handle this? There are a lot of different cache classes
        return new ReflectiveClassBuildItem(false, false,
                "com.github.benmanes.caffeine.cache.SSLMS",
                "com.github.benmanes.caffeine.cache.SILMS",
                "com.github.benmanes.caffeine.cache.PSMS",
                "com.github.benmanes.caffeine.cache.PDMS",
                "com.github.benmanes.caffeine.cache.SSMS",
                "com.github.benmanes.caffeine.cache.SSLA",
                "com.github.benmanes.caffeine.cache.PSA");
    }

    @BuildStep
    void cacheLoaders(CombinedIndexBuildItem combinedIndex, BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, CACHE_LOADER_CLASS_NAME));

        for (ClassInfo info : combinedIndex.getIndex().getAllKnownImplementors(CACHE_LOADER_NAME)) {
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, info.name().toString()));
        }
    }
}
