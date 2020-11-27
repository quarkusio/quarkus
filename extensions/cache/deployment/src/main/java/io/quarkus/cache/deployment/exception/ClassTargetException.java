package io.quarkus.cache.deployment.exception;

import org.jboss.jandex.DotName;

/**
 * This exception is thrown at build time during the validation phase if a class is annotated with
 * {@link io.quarkus.cache.CacheInvalidate @CacheInvalidate}, {@link io.quarkus.cache.CacheInvalidateAll @CacheInvalidateAll} or
 * {@link io.quarkus.cache.CacheResult @CacheResult}. These annotations are only allowed at type level for the caching
 * interceptors from this extension.
 */
@SuppressWarnings("serial")
public class ClassTargetException extends RuntimeException {

    private final DotName className;

    public ClassTargetException(DotName className, DotName annotationName) {
        super("Caching annotations are not allowed on a class [class=" + className + ", annotation=" + annotationName + "]");
        this.className = className;
    }

    public DotName getClassName() {
        return className;
    }
}
