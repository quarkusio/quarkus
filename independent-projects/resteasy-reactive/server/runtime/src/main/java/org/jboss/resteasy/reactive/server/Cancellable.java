package org.jboss.resteasy.reactive.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.container.ContainerResponseFilter;

import io.smallrye.mutiny.Uni;

/**
 * Used on a method that returns a single item async return type (such as {@link Uni} or {@link CompletionStage or Kotlin
 * suspend function})
 * to control whether to cancel the subscription to the result if the connection is closed before the result is ready.
 * By default, Quarkus will cancel the subscription.
 * <p>
 * Can also be placed on a {@link ContainerResponseFilter} class with {@code value = false}
 * to ensure the filter is still executed even when the client closes the connection before the response is completed.
 * Only filters explicitly marked as non-cancellable will run; other response filters are still skipped.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Cancellable {

    boolean value() default true;
}
