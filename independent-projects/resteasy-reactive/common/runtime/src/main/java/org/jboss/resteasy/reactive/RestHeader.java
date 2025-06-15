package org.jboss.resteasy.reactive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Equivalent of HeaderParam but with optional name.
 * <p>
 * When the name is not specified, then the parameter name is converted to kebab case while the first letter of every
 * part of the kebab is made uppercase.
 * <p>
 * For example {@code @RestHeader String iAmTheParam} results in the value of the HTTP header {@code I-Am-The-Param}
 * being used.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
public @interface RestHeader {
    String value() default "";
}
