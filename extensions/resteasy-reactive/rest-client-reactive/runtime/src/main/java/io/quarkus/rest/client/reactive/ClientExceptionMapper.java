package io.quarkus.rest.client.reactive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.Priorities;

/**
 * Used to easily define an exception mapper for the specific REST Client on which it's used.
 * This method is called when the HTTP response from the invoked service has a status code of 400 or higher.
 *
 * The annotation MUST be placed on a method of the REST Client interface that meets the following criteria:
 * <ul>
 * <li>Is a {@code static} method</li>
 * <li>Returns any subclass of {@link RuntimeException}</li>
 * <li>Takes a single parameter of type {@link jakarta.ws.rs.core.Response}</li>
 * </ul>
 *
 * An example method could look like the following:
 *
 * <pre>
 * {@code
 * &#64;ClientExceptionMapper
 * static DummyException map(Response response) {
 *     if (response.getStatus() == 404) {
 *         return new DummyException();
 *     }
 *     return null;
 * }
 *
 * }
 * </pre>
 *
 * If {@code null} is returned, Quarkus will continue searching for matching
 * {@link org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ClientExceptionMapper {

    /**
     * The priority with which the exception mapper will be executed
     */
    int priority() default Priorities.USER;
}
