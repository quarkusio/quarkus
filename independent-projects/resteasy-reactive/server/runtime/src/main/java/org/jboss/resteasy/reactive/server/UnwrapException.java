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
 * The unwrapping behavior is controlled by the {@link #strategy()} attribute:
 * <ul>
 * <li>{@link ExceptionUnwrapStrategy#ALWAYS}: Always unwraps before checking mappers. Only falls back to
 * checking mappers for the wrapper if no mapper is found for any unwrapped cause.</li>
 * <li>{@link ExceptionUnwrapStrategy#UNWRAP_IF_NO_EXACT_MATCH}: Unwraps only if the thrown exception type
 * itself has no exact mapper. Checks for exact match first, then unwraps if none found.</li>
 * <li>{@link ExceptionUnwrapStrategy#UNWRAP_IF_NO_MATCH} (default): Unwraps only if neither the thrown
 * exception nor any of its supertypes have a registered mapper.</li>
 * </ul>
 * <p>
 * Example:
 *
 * <pre>
 * &#64;UnwrapException(value = { WebApplicationException.class }, strategy = ExceptionUnwrapStrategy.UNWRAP_IF_NO_EXACT_MATCH)
 * public class ExceptionsMappers {
 *     &#64;ServerExceptionMapper
 *     public RestResponse&lt;Error&gt; mapUnhandledException(RuntimeException ex) {
 *         // Handles RuntimeException and its subclasses
 *     }
 *
 *     &#64;ServerExceptionMapper
 *     public RestResponse&lt;Error&gt; mapJsonProcessingException(JsonProcessingException ex) {
 *         // This will be called for JsonProcessingException wrapped in WebApplicationException
 *         // because UNWRAP_IF_NO_EXACT_MATCH forces unwrapping even though RuntimeException mapper exists
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
     * The unwrapping strategy to use for this exception.
     * <p>
     * Defaults to {@link ExceptionUnwrapStrategy#UNWRAP_IF_NO_MATCH}.
     */
    ExceptionUnwrapStrategy strategy() default ExceptionUnwrapStrategy.UNWRAP_IF_NO_MATCH;
}
