package io.quarkus.rest.client.reactive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.Priorities;

/**
 * Used to easily define a custom redirect handler for the specific REST Client on which it's used.
 * This method is called when the HTTP response from the invoked service has a status code of 300 or higher.
 *
 * The annotation MUST be placed on a method of the REST Client interface that meets the following criteria:
 * <ul>
 * <li>Is a {@code static} method</li>
 * <li>Returns any subclass of {@link java.net.URI}</li>
 * <li>Takes a single parameter of type {@link jakarta.ws.rs.core.Response}</li>
 * </ul>
 *
 * An example method could look like the following:
 *
 * <pre>
 * {@code
 * &#64;ClientRedirectHandler
 * static DummyException map(Response response) {
 *     if (response.getStatus() == 307) {
 *         return response.getLocation();
 *     }
 *     // no redirect
 *     return null;
 * }
 *
 * }
 * </pre>
 *
 * If {@code null} is returned, Quarkus will not redirect
 * {@link org.jboss.resteasy.reactive.client.handlers.RedirectHandler}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ClientRedirectHandler {

    /**
     * The priority with which the redirect handler will be executed
     */
    int priority() default Priorities.USER;
}
