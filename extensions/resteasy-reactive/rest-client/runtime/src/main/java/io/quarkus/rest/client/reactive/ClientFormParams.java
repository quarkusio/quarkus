package io.quarkus.rest.client.reactive;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to specify form parameters that should be sent with the outbound request.
 * When this annotation is placed at the interface level of a REST client interface, the specified form parameters will be sent
 * on each request for all methods in the interface.
 * When this annotation is placed on a method, the parameters will be sent only for that method. If the same form parameter is
 * specified in an annotation for both the type and the method, only the form value specified in the annotation on the method
 * will be sent.
 * <p>
 * This class serves to act as the {@link java.lang.annotation.Repeatable} implementation for {@link ClientFormParam}.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ClientFormParams {
    ClientFormParam[] value();
}
