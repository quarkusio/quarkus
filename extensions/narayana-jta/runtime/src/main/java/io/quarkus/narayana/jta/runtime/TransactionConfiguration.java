package io.quarkus.narayana.jta.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to configure a different transaction timeout than the default one for a method or a class.
 * <p>
 * When defined on a method, it needs to be used on the entry method of the transaction.
 * <p>
 * If defined on a class, it is equivalent to defining it on all the methods of the class marked with {@code @Transactional}.
 * The configuration defined on a method takes precedence over the configuration defined on a class.
 */
@Inherited
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface TransactionConfiguration {

    /**
     * This value is used to specify that no transaction timeout is configured.
     */
    int UNSET_TIMEOUT = -1;

    /**
     * The transaction timeout in seconds.
     * Defaults to UNSET_TIMEOUT: no timeout configured.
     *
     * @return The transaction timeout in seconds.
     */
    int timeout() default UNSET_TIMEOUT;

    String UNSET_TIMEOUT_CONFIG_PROPERTY = "<<unset>>";

    /**
     * The configuration property to use in order to determine the value of the timeout in seconds.
     * If the property exists, it must be an integer value representing the transaction timeout in seconds.
     *
     * An example configuration in {@code application.properties} could be: {@code my-transaction.timeout=5}.
     *
     * If both {@code timeoutFromConfigProperty} and {@code timeout} are set, then Quarkus will attempt to resolve
     * {@code timeoutFromConfigProperty} and if a value for it has been provided, the timeout is set to that value.
     * If no value has been provided at runtime for the property, then the value of {@code timeout} will be used
     * as the fallback.
     */
    String timeoutFromConfigProperty() default UNSET_TIMEOUT_CONFIG_PROPERTY;
}
