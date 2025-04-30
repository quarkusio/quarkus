package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation used to indicate that a recorder method is called during the static init phase
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface StaticInit {
}
