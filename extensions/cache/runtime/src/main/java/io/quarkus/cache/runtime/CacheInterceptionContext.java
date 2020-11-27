package io.quarkus.cache.runtime;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class CacheInterceptionContext<T extends Annotation> {

    private final List<T> interceptorBindings = new ArrayList<>();
    private short[] cacheKeyParameterPositions = new short[0];

    public List<T> getInterceptorBindings() {
        return interceptorBindings;
    }

    public short[] getCacheKeyParameterPositions() {
        return cacheKeyParameterPositions;
    }

    public void setCacheKeyParameterPositions(short[] cacheKeyParameterPositions) {
        this.cacheKeyParameterPositions = cacheKeyParameterPositions;
    }
}
