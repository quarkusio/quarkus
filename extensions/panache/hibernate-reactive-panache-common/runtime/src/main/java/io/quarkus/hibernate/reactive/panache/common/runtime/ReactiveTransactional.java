package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

import org.hibernate.reactive.mutiny.Mutiny;

import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.VertxThread;

/**
 * Use this annotation on your method to run them in a reactive {@link Mutiny.Session.Transation}.
 * 
 * If the annotated method returns a {@link Uni}, this has exactly the same behaviour as if the method
 * was enclosed in a call to {@link Mutiny.Session#withTransaction(java.util.function.Function)}.
 * 
 * Otherwise, invocations are only allowed when not running from a {@link VertxThread} and the behaviour
 * will be the same as a blocking call to {@link Mutiny.Session#withTransaction(java.util.function.Function)}.
 */
@Inherited
@InterceptorBinding
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ReactiveTransactional {

}
