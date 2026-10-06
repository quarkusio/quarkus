package io.quarkus.hibernate.accessor.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers a field, getter/setter method, constructor or type for the Hibernate accessor factory at build time.
 * Annotating a type registers its declared instance fields, no-argument methods returning a value,
 * one-argument methods and, for concrete non-enum classes, constructors. Inherited members must be registered separately.
 * <p>
 * Annotated getters must take no arguments and return a value; setters must take one argument,
 * and any return value is ignored. Record fields are read-only.
 * <p>
 * The configured accessor strategy determines whether registered members use generated accessors or reflection.
 * This annotation is retained in class files for build-time indexing and is not available through runtime reflection.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.TYPE })
public @interface ReflectionFreeAccessor {
}
