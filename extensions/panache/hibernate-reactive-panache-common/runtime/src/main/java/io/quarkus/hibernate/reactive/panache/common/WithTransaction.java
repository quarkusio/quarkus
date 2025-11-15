package io.quarkus.hibernate.reactive.panache.common;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

/**
 * Instructs Panache to trigger the {@link io.smallrye.mutiny.Uni} returned from the intercepted method within a scope of a
 * reactive {@link org.hibernate.reactive.mutiny.Mutiny.Transaction}.
 * <p>
 * If a reactive session exists when the {@link io.smallrye.mutiny.Uni} returned from the annotated method is triggered, then
 * this session is reused. Otherwise, a new
 * session is opened and eventually closed when the {@link io.smallrye.mutiny.Uni} completes.
 * <p>
 * A method annotated with this annotation must return {@link io.smallrye.mutiny.Uni}. If declared on a class then all methods
 * that return {@link io.smallrye.mutiny.Uni} are considered; all other methods are ignored.
 *
 * @see org.hibernate.reactive.mutiny.Mutiny.SessionFactory#withTransaction(java.util.function.Function)
 */
@Inherited
@InterceptorBinding
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface WithTransaction {

    /**
     * The name of the persistence unit. If not specified, the default persistence unit is used.
     *
     * @return the persistence unit name
     */
    @Nonbinding
    String value() default DEFAULT_PERSISTENCE_UNIT_NAME;

}
