package io.quarkus.vertx.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.vertx.core.http.HttpServerRequest;

/**
 * Identifies a route method parameter that should be injected with a value returned from
 * {@link HttpServerRequest#getParam(String)}.
 * <p>
 * The parameter type must be {@link String}, {@code java.util.Optional<String>} or {@code java.util.List<String>}, otherwise
 * the build fails.
 * 
 * @see HttpServerRequest#getParam(String)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Param {

    /**
     * Constant value for {@link #value()} indicating that the annotated element's name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    /**
     * The name of the parameter. By default, the element's name is used as-is.
     *
     * @return the name of the parameter
     */
    String value() default ELEMENT_NAME;

}
