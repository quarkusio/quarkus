package io.quarkus.rest.client.reactive;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows for a per invocation override the base URL.
 * At most a one such annotation can be used per REST Client method and the supported types are:
 *
 * <ul>
 * {@link String}</li>
 * {@link java.net.URI}</li>
 * {@link java.net.URL}</li>
 * </ul>
 */
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Url {
}
