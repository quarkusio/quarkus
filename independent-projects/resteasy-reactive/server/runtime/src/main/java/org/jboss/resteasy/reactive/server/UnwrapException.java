package org.jboss.resteasy.reactive.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to configure that an exception (or exceptions) should be unwrapped during exception handling.
 * <p>
 * Unwrapping means that when an {@link Exception} of the configured type is thrown and no
 * {@code jakarta.ws.rs.ext.ExceptionMapper} exists,
 * then RESTEasy Reactive will attempt to locate an {@code ExceptionMapper} for the cause of the Exception.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UnwrapException {

    /**
     * If this is not set, the value is assumed to be the exception class where the annotation is placed
     */
    Class<? extends Exception>[] value() default {};
}
