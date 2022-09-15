package org.jboss.resteasy.reactive;

import jakarta.ws.rs.core.Response;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When placed on a resource method, then RESTEasy Reactive will set the HTTP status to the specified value,
 * if the method completes without an exception and if it does not return {@link Response} or {@link RestResponse}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ResponseStatus {

    int value();
}
