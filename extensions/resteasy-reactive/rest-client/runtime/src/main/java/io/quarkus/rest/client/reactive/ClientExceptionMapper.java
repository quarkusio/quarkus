package io.quarkus.rest.client.reactive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.Priorities;

/**
 * Used to easily define an exception mapper for the specific REST Client on which it's used.
 * This method is called when the HTTP response from the invoked service has a status code of 400 or higher.
 * <p>
 * The annotation MUST be placed on a method of the REST Client interface that meets the following criteria:
 *
 * <ul>
 * <li>Is a {@code static} method</li>
 * <li>Returns any subclass of {@link RuntimeException}</li>
 * </ul>
 *
 * The method can utilize any combination of the following parameters:
 *
 * <ul>
 * <li>{@code jakarta.ws.rs.core.Response} which represents the HTTP response</li>
 * <li>{@code Method} which represents the invoked method of the client</li>
 * <li>{@code URI} which represents the the request URI</li>
 * <li>{@code Map<String, Object>} which gives access to the properties that are available to (and potentially changed by)
 * {@link jakarta.ws.rs.client.ClientRequestContext}</li>
 * <li>{@code jakarta.ws.rs.core.MultivaluedMap} containing the request headers</li>
 * </ul>
 *
 * An example method could look like the following:
 *
 * <pre>
 * {@code
 * @ClientExceptionMapper
 * static DummyException map(Response response, Method method) {
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
     * The priority with which the exception mapper will be executed.
     * <p>
     * They are sorted in ascending order; the lower the number the higher the priority.
     *
     * @see Priorities
     */
    int priority() default Priorities.USER;
}
