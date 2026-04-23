package io.quarkus.signals.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the ordering of a component relative to other components of the same type.
 * <p>
 * Components are referenced by their {@link io.smallrye.common.annotation.Identifier} value.
 * A component listed in {@link #before()} has lower priority than this component, i.e., this component is processed first.
 * A component listed in {@link #after()} has higher priority than this component.
 * <p>
 * Cycles in the ordering are detected at build time and result in a deployment error.
 */
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ComponentOrder {

    /**
     * Identifiers of components that this component takes precedence over (i.e., this component has higher priority).
     *
     * @return the component identifiers
     */
    String[] before() default {};

    /**
     * Identifiers of components that take precedence over this component (i.e., this component has lower priority).
     *
     * @return the component identifiers
     */
    String[] after() default {};

}
