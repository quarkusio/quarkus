package io.quarkus.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

/**
 * Indicates that this method should be run in a rollback only reactive transaction.
 *
 * This allows the test method to modify the database as required, and then have
 * these changes reverted at the end of the method.
 *
 * @deprecated Use {@link TestTransaction} instead. When used on a method that returns
 *             {@link io.smallrye.mutiny.Uni}, {@code @TestTransaction} automatically uses
 *             a reactive transaction with rollback.
 */
@Deprecated(forRemoval = true, since = "4.0.0")
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface TestReactiveTransaction {
}
