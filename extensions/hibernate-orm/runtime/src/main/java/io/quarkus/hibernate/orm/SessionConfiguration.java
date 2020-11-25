package io.quarkus.hibernate.orm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.narayana.jta.runtime.AdditionalTransactionConfiguration;

/**
 * This annotation can be used to configure the Hibernate session.
 */
@Inherited
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(value = RetentionPolicy.RUNTIME)
@AdditionalTransactionConfiguration
public @interface SessionConfiguration {
    /**
     * Whether or not the transaction performs read only operations on the underlying transactional resource.
     * Depending on the transactional resource, optimizations can be performed in case of read only transactions.
     *
     * @return true if read only.
     */
    boolean readOnly() default false;
}
