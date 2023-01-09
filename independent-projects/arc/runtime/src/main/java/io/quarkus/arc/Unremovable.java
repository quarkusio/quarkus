package io.quarkus.arc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the bean marked with this annotation should never be removed by Arc
 * even if it's considered unused.
 *
 * An unused bean:
 * <ul>
 * <li>is not a built-in bean or an interceptor</li>
 * <li>is not eligible for injection to any injection point,</li>
 * <li>is not excluded by any extension,</li>
 * <li>does not have a name,</li>
 * <li>does not declare an observer,</li>
 * <li>does not declare any producer which is eligible for injection to any injection point,</li>
 * <li>is not directly eligible for injection into any `jakarta.enterprise.inject.Instance` or `jakarta.inject.Provider`
 * injection
 * point</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
public @interface Unremovable {
}
