package io.quarkus.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

/**
 * Indicates that this method should be run in a rollback only reactive transaction.
 *
 * This allows the test method to modify the database as required, and then have
 * these changes reverted at the end of the method.
 * 
 * @see TestTransaction for JTA transactions.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface TestReactiveTransaction {
}
