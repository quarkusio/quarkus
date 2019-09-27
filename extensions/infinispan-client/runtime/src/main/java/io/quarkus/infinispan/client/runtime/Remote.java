package io.quarkus.infinispan.client.runtime;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

/**
 * Qualifier used to specify which remote cache will be injected.
 *
 * @author William Burns
 * @deprecated use {@link io.quarkus.infinispan.client.Remote} instead
 */
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Retention(RUNTIME)
@Documented
@Qualifier
@Deprecated
public @interface Remote {
    /**
     * The remote cache name. If no value is provided the default cache is assumed.
     */
    @Nonbinding
    String value() default "";
}
