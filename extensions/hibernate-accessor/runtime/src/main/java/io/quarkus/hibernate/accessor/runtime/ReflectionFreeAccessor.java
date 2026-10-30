package io.quarkus.hibernate.accessor.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers a type's declared instance members for the Hibernate accessor factory at build time.
 * Inherited members must be registered separately. Member annotations override these category settings.
 * Independent extension registrations remain additive.
 * <p>
 * By default fields are readable, zero-argument non-void methods are readers, one-argument methods
 * are writers (their return value is ignored), and constructors of concrete non-enum classes are registered.
 * Generated field writers remove final; reflection-only access does not change modifiers.
 * Record-field write requests fail the build under every strategy.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ReflectionFreeAccessor {
    ReflectionFreeFieldAccessor fields() default @ReflectionFreeFieldAccessor;

    ReflectionFreeMethodAccessor methods() default @ReflectionFreeMethodAccessor;

    ReflectionFreeConstructorAccessor constructors() default @ReflectionFreeConstructorAccessor;
}
