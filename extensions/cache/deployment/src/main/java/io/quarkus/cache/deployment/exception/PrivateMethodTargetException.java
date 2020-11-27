package io.quarkus.cache.deployment.exception;

import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

/**
 * This exception is thrown at build time during the validation phase if a private method is annotated with
 * {@link io.quarkus.cache.CacheInvalidate @CacheInvalidate}, {@link io.quarkus.cache.CacheInvalidateAll @CacheInvalidateAll} or
 * {@link io.quarkus.cache.CacheResult @CacheResult}.
 */
@SuppressWarnings("serial")
public class PrivateMethodTargetException extends RuntimeException {

    private final MethodInfo methodInfo;

    public PrivateMethodTargetException(MethodInfo methodInfo, DotName annotationName) {
        super("Caching annotations are not allowed on a private method [class=" + methodInfo.declaringClass().name()
                + ", method=" + methodInfo.name() + ", annotation=" + annotationName + "]");
        this.methodInfo = methodInfo;
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }
}
