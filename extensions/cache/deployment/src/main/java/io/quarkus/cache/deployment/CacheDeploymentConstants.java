package io.quarkus.cache.deployment;

import java.util.Arrays;
import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;

public class CacheDeploymentConstants {

    // API annotations names.
    public static final DotName CACHE_INVALIDATE_ALL = dotName(CacheInvalidateAll.class);
    public static final DotName CACHE_INVALIDATE_ALL_LIST = dotName(CacheInvalidateAll.List.class);
    public static final DotName CACHE_INVALIDATE = dotName(CacheInvalidate.class);
    public static final DotName CACHE_INVALIDATE_LIST = dotName(CacheInvalidate.List.class);
    public static final DotName CACHE_RESULT = dotName(CacheResult.class);
    public static final DotName CACHE_KEY = dotName(CacheKey.class);
    public static final List<DotName> API_METHODS_ANNOTATIONS = Arrays.asList(
            CACHE_RESULT, CACHE_INVALIDATE, CACHE_INVALIDATE_ALL);
    public static final List<DotName> API_METHODS_ANNOTATIONS_LISTS = Arrays.asList(
            CACHE_INVALIDATE_LIST, CACHE_INVALIDATE_ALL_LIST);

    // Annotations parameters.
    public static final String CACHE_NAME_PARAM = "cacheName";
    public static final String CACHE_KEY_PARAMETER_POSITIONS_PARAM = "cacheKeyParameterPositions";
    public static final String LOCK_TIMEOUT_PARAM = "lockTimeout";

    // Caffeine.
    public static final String CAFFEINE_CACHE_TYPE = "caffeine";

    private static DotName dotName(Class<?> annotationClass) {
        return DotName.createSimple(annotationClass.getName());
    }
}
