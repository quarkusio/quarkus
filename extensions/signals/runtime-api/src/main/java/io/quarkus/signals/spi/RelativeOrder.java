package io.quarkus.signals.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * Defines the ordering of a component relative to other components of the same type.
 * <p>
 * Components are referenced by their {@link io.smallrye.common.annotation.Identifier} value.
 * <p>
 * Cycles in the ordering are detected at build time and result in a deployment error.
 */
@Experimental("This API is experimental and may change in the future")
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RelativeOrder {

    /**
     * Identifiers of components that this component takes precedence over, i.e., are processed after this component.
     *
     * @return the component identifiers
     */
    String[] before() default {};

    /**
     * Identifiers of components that take precedence over this component, i.e., are processed before this component.
     *
     * @return the component identifiers
     */
    String[] after() default {};

}
