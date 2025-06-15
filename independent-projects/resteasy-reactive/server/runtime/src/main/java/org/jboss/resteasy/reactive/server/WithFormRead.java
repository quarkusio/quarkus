package org.jboss.resteasy.reactive.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Forces the form body to be read and parsed before filters and the endpoint are invoked. This is only useful if your
 * endpoint does not contain any declared form parameter, which would otherwise force the form body being read anyway.
 * You can place this annotation on request filters as well as endpoints.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WithFormRead {

}
