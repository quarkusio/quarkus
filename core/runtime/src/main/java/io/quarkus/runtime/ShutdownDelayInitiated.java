package io.quarkus.runtime;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.ObserverMethod;

/**
 * This annotation is used to mark a business method of a CDI bean that should be executed when the shutdown delay starts.
 * The annotated method must be non-private and non-static and declare no arguments.
 * Furthermore, {@code quarkus.shutdown.delay-enabled} must be set to {@code true}.
 * <p>
 * The behavior is similar to a declaration of a {@link ShutdownDelayInitiatedEvent} observer. In fact, a synthetic observer of
 * the
 * {@link ShutdownDelayInitiatedEvent} is generated for each occurrence of this annotation. Within the observer, the contextual
 * instance of a
 * bean is obtained first, and then the method is invoked.
 * <p>
 * Furthermore, {@link #value()} can be used to specify the priority of the generated observer method and thus affects observers
 * ordering.
 * <p>
 * The contextual instance is destroyed immediately after the method is invoked for {@link Dependent} beans.
 * <p>
 * The following examples are functionally equivalent.
 *
 * <pre>
 * &#064;ApplicationScoped
 * class Bean1 {
 *     void onShutdown(&#064;Observes ShutdownDelayInitiatedEvent event) {
 *         // place the logic here
 *     }
 * }
 *
 * &#064;ApplicationScoped
 * class Bean2 {
 *
 *     &#064;ShutdownDelayInitiated
 *     void shutdown() {
 *         // place the logic here
 *     }
 * }
 * </pre>
 *
 * @see ShutdownDelayInitiatedEvent
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface ShutdownDelayInitiated {

    /**
     *
     * @return the priority
     * @see jakarta.annotation.Priority
     */
    int value() default ObserverMethod.DEFAULT_PRIORITY;
}
