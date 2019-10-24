package io.quarkus.security.jpa;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Indicates that this field or property should be used as a source of roles for security when you store
 * roles in a separate table from your user entity.
 * Supports only the {@link String} type.
 * </p>
 * <p>
 * Each role element is considered a comma-separated list of roles.
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RolesValue {

}
