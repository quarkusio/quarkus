package io.quarkus.caffeine.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

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
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, CACHE_LOADER_CLASS_NAME));
            for (String name : effectiveImplementorNames) {
                reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, name));
            }
        }
    }
}
