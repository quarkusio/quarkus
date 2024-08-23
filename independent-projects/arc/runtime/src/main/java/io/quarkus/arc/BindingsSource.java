package io.quarkus.arc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a source of interceptor binding annotations. When this annotation
 * is present on an {@link InterceptionProxy} parameter of a CDI producer method,
 * the interceptor binding annotations present on the target class are ignored
 * and instead are read from the class defined by this annotation.
 * <p>
 * Class-level interceptor bindings of the target class are equal to class-level
 * interceptor bindings on the bindings source class.
 * <p>
 * Method-level interceptor bindings of a method on the target class are equal to
 * method-level bindings of a method with the same name, return type, parameter
 * types and {@code static} flag declared on the bindings source class.
 * <p>
 * These annotations can be present on the {@link #value()} class:
 * <p>
 * <ul>
 * <li><em>interceptor bindings</em>: on the class and on the methods</li>
 * <li><em>stereotypes</em>: on the class</li>
 * <li>{@link NoClassInterceptors}: on the methods</li>
 * </ul>
 * <p>
 * Other annotations on the {@code value()} class are ignored.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface BindingsSource {
    /**
     * The class from which interceptor binding annotations are read.
     */
    Class<?> value();
}
