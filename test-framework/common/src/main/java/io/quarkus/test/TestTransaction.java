package io.quarkus.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

/**
 * Indicates that this method should be run in a rollback-only transaction.
 * <p>
 * For imperative tests, a JTA transaction is used.
 * <p>
 * For reactive tests (when {@code quarkus-hibernate-reactive} is present), test methods
 * that return {@code Uni} automatically run on a Vert.x event loop context with a reactive
 * transaction that is rolled back at the end.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface TestTransaction {
}
