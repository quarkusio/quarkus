package io.quarkus.cache.deployment;

import java.util.Arrays;
import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.runtime.CacheInvalidateAllInterceptor;
import io.quarkus.cache.runtime.CacheInvalidateInterceptor;
import io.quarkus.cache.runtime.CacheKeyParameterPositions;
import io.quarkus.cache.runtime.CacheResultInterceptor;
import io.smallrye.mutiny.Multi;

public class CacheDeploymentConstants {

    // API annotations names.
    public static final DotName CACHE_NAME = dotName(CacheName.class);
    public static final DotName CACHE_INVALIDATE_ALL = dotName(CacheInvalidateAll.class);
    public static final DotName CACHE_INVALIDATE_ALL_LIST = dotName(CacheInvalidateAll.List.class);
    public static final DotName CACHE_INVALIDATE = dotName(CacheInvalidate.class);
    public static final DotName CACHE_INVALIDATE_LIST = dotName(CacheInvalidate.List.class);
    public static final DotName CACHE_RESULT = dotName(CacheResult.class);
    public static final DotName CACHE_KEY = dotName(CacheKey.class);
    public static final List<DotName> INTERCEPTOR_BINDINGS = Arrays.asList(CACHE_RESULT, CACHE_INVALIDATE,
            CACHE_INVALIDATE_ALL);
    public static final List<DotName> INTERCEPTOR_BINDING_CONTAINERS = Arrays.asList(CACHE_INVALIDATE_LIST,
            CACHE_INVALIDATE_ALL_LIST);
    public static final List<DotName> INTERCEPTORS = Arrays.asList(dotName(CacheInvalidateAllInterceptor.class),
            dotName(CacheInvalidateInterceptor.class), dotName(CacheResultInterceptor.class));
    public static final DotName CACHE_KEY_PARAMETER_POSITIONS = dotName(CacheKeyParameterPositions.class);

    // MicroProfile REST Client.
    public static final DotName REGISTER_REST_CLIENT = DotName
            .createSimple("org.eclipse.microprofile.rest.client.inject.RegisterRestClient");

    // Mutiny.
    public static final DotName MULTI = dotName(Multi.class);

    // Annotations parameters.
    public static final String CACHE_NAME_PARAM = "cacheName";

    private static DotName dotName(Class<?> annotationClass) {
        return DotName.createSimple(annotationClass.getName());
    }
}
