package io.quarkus.narayana.jta.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to configure a different transaction timeout that the default one for a method or a class.
 * It needs to be used on the entry method of the transaction.
 */
@Inherited
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface TransactionConfiguration {
    /** The value is used to specify that no transaction timeout is configured */
    int UNSET_TIMEOUT = -1;

    /**
     * The transaction timeout in second.
     * Defaults to UNSET_TIMEOUT: no timeout configured.
     * 
     * @return The transaction timeout in second.
     */
    int timeout() default UNSET_TIMEOUT;
}
