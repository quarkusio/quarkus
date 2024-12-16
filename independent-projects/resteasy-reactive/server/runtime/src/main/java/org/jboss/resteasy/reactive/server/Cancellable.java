package org.jboss.resteasy.reactive.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CompletionStage;

import io.smallrye.mutiny.Uni;

/**
 * Used on a method that returns a single item async return type (such as {@link Uni} or {@link CompletionStage or Kotlin
 * suspend function})
 * to control whether to cancel the subscription to the result if the connection is closed before the result is ready.
 * By default, Quarkus will cancel the subscription
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Cancellable {

    boolean value() default true;
}
