package io.quarkus.arc.profile;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When applied to a bean class or producer method (or field), the bean will only be enabled
 * if the Quarkus build time profile does not match the specified annotation value.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.FIELD })
public @interface UnlessBuildProfile {

    String value();
}
