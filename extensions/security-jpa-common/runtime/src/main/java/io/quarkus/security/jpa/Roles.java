package io.quarkus.security.jpa;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

/**
 * <p>
 * Indicates that this field or property should be used as a source of roles for security.
 * Supports the {@link String} type, or a {@code Collection<String>} or a {@link Collection} of
 * entities with a field or getter annotated with {@link RolesValue}.
 * </p>
 * <p>
 * Each role element is considered a comma-separated list of roles.
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Roles {

}
