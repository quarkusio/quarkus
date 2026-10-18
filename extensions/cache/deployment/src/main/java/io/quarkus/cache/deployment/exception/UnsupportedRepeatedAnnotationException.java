package io.quarkus.cache.deployment.exception;

import org.jboss.jandex.MethodInfo;

/**
 * This exception is thrown at build time during the validation phase if a method from a MicroProfile REST Client bean is
 * annotated with repeated {@link io.quarkus.cache.CacheInvalidate @CacheInvalidate} or
 * {@link io.quarkus.cache.CacheInvalidateAll @CacheInvalidateAll} annotations. Interceptions on such a bean are not managed by
 * Arc and the usage of repeated interceptor bindings is not currently supported.
 */
@SuppressWarnings("serial")
public class UnsupportedRepeatedAnnotationException extends RuntimeException {

    private final MethodInfo methodInfo;

    public UnsupportedRepeatedAnnotationException(MethodInfo methodInfo) {
        super("Repeated caching annotations on a method from a class annotated with @RegisterRestClient are not currently supported [class="
                + methodInfo.declaringClass().name() + ", method=" + methodInfo.name() + "]");
        this.methodInfo = methodInfo;
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }
}
