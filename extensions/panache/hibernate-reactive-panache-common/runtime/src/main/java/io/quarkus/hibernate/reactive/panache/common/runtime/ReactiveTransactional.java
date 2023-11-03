package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

/**
 * Use this annotation on your method to run them in a reactive {@link Mutiny.Transaction}.
 * <p>
 * The annotated method must return a {@link Uni}.
 *
 * @deprecated Use {@link WithTransaction} instead.
 */
@Deprecated(forRemoval = true)
@Inherited
@InterceptorBinding
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ReactiveTransactional {

}
