package io.quarkus.runtime.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Adds metadata to a JsonRPC method to control its behavior and appearance.
 */
@Retention(RUNTIME)
@Target({ METHOD, PARAMETER })
@Documented
public @interface JsonRpcDescription {

    /**
     * @return the description text
     */
    String value();

}