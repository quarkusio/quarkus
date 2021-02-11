package io.quarkus.cache.runtime;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CacheInterceptionContext<T extends Annotation> {

    private final List<T> interceptorBindings;
    private final List<Short> cacheKeyParameterPositions;

    public CacheInterceptionContext(List<T> interceptorBindings, List<Short> cacheKeyParameterPositions) {
        Objects.requireNonNull(interceptorBindings);
        Objects.requireNonNull(cacheKeyParameterPositions);
        this.interceptorBindings = Collections.unmodifiableList(interceptorBindings);
        this.cacheKeyParameterPositions = Collections.unmodifiableList(cacheKeyParameterPositions);
    }

    public List<T> getInterceptorBindings() {
        return interceptorBindings;
    }

    public List<Short> getCacheKeyParameterPositions() {
        return cacheKeyParameterPositions;
    }
}
