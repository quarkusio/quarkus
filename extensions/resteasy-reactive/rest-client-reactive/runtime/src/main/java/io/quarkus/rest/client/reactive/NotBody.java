package io.quarkus.rest.client.reactive;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The sole purpose of this annotation is to allow REST Client methods to contain multiple non-annotated Jakarta REST parameters
 * which would normally not be allowed because all the parameters would be considered to represent the body of the request.
 */
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NotBody {
}
