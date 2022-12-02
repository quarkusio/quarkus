package io.quarkus.infinispan.client.runtime.cache;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CacheInterceptionContext<T extends Annotation> {

    private final List<T> interceptorBindings;

    public CacheInterceptionContext(List<T> interceptorBindings) {
        Objects.requireNonNull(interceptorBindings);
        this.interceptorBindings = Collections.unmodifiableList(interceptorBindings);
    }

    public List<T> getInterceptorBindings() {
        return interceptorBindings;
    }

}
