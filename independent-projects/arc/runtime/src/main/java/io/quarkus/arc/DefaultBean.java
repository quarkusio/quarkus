package io.quarkus.arc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @deprecated Use {@link jakarta.enterprise.inject.Reserve}
 *             and don't forget to add a {@link jakarta.annotation.Priority}.
 *             Reserve beans without priority are not registered. In essence,
 *             {@code @DefaultBean} is equivalent to {@code @Reserve @Priority(0)}.
 */
@Deprecated(forRemoval = true, since = "4.0")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.FIELD })
public @interface DefaultBean {

}
