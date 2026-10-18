package io.quarkus.security.jpa;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that this entity class should be used as a source of identity information. At most one
 * entity can have that annotation in an application. The entity must contain fields or properties annotated
 * with the {@link Username}, {@link Password} and {@link Roles} annotations.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UserDefinition {

}
