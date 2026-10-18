package io.quarkus.cache.deployment.exception;

import org.jboss.jandex.MethodInfo;

/**
 * This exception is thrown at build time during the validation phase if a method returning void is annotated with
 * {@link io.quarkus.cache.CacheResult @CacheResult}.
 */
@SuppressWarnings("serial")
public class VoidReturnTypeTargetException extends RuntimeException {

    private final MethodInfo methodInfo;

    public VoidReturnTypeTargetException(MethodInfo methodInfo) {
        super("@CacheResult is not allowed on a method returning void [class=" + methodInfo.declaringClass().name()
                + ", method=" + methodInfo.name() + "]");
        this.methodInfo = methodInfo;
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }
}
