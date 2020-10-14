package io.quarkus.caffeine.deployment;

import java.io.IOException;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class CaffeineProcessor {

    private static final DotName BOUNDED_LOCAL_CACHE_NAME = DotName
            .createSimple("com.github.benmanes.caffeine.cache.BoundedLocalCache");
    private static final DotName NODE_NAME = DotName.createSimple("com.github.benmanes.caffeine.cache.Node");
    private static final DotName CACHE_LOADER_NAME = DotName.createSimple("com.github.benmanes.caffeine.cache.CacheLoader");

    @BuildStep
    void cacheClasses(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws IOException {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                combinedIndex.getIndex().getAllKnownSubclasses(BOUNDED_LOCAL_CACHE_NAME).stream()
                        .map(ci -> ci.name().toString())
                        .toArray(String[]::new)));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                combinedIndex.getIndex().getAllKnownSubclasses(NODE_NAME).stream()
                        .map(ci -> ci.name().toString())
                        .toArray(String[]::new)));
    }

    @BuildStep
    void cacheLoaders(CombinedIndexBuildItem combinedIndex, BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, CACHE_LOADER_NAME.toString()));

        for (ClassInfo info : combinedIndex.getIndex().getAllKnownImplementors(CACHE_LOADER_NAME)) {
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, info.name().toString()));
        }
    }
}
