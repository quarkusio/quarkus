package org.jboss.resteasy.reactive.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to configure that an exception (or exceptions) should be unwrapped during exception handling.
 * <p>
 * Unwrapping means that when an {@link Exception} of the configured type is thrown,
 * RESTEasy Reactive will attempt to locate an {@code ExceptionMapper} for the cause of the Exception.
 * <p>
 * By default ({@code always = false}), unwrapping only occurs when no {@code jakarta.ws.rs.ext.ExceptionMapper}
 * exists for the exception or its parent types.
 * <p>
 * When {@code always = true}, unwrapping occurs even if an {@code ExceptionMapper} exists for one of the exception
 * parent types, but not if the exception is directly mapped.
 * <p>
 * Example:
 *
 * <pre>
 * &#64;UnwrapException(value = { WebApplicationException.class }, always = true)
 * public class ExceptionsMappers {
 *     &#64;ServerExceptionMapper
 *     public RestResponse&lt;Error&gt; mapUnhandledException(RuntimeException ex) {
 *         // Handles RuntimeException and its subclasses
 *     }
 *
 *     &#64;ServerExceptionMapper
 *     public RestResponse&lt;Error&gt; mapJsonProcessingException(JsonProcessingException ex) {
 *         // This will be called for JsonProcessingException wrapped in WebApplicationException
 *         // because always=true forces unwrapping even though RuntimeException mapper exists
 *     }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UnwrapException {

    /**
     * If this is not set, the value is assumed to be the exception class where the annotation is placed
     */
    Class<? extends Exception>[] value() default {};

    /**
     * When {@code true}, the exception is always unwrapped even if an {@code ExceptionMapper} exists
     * for the exception or its parent types.
     * <p>
     * When {@code false} (default), unwrapping only occurs when no {@code ExceptionMapper} exists.
     */
    boolean always() default false;
}
