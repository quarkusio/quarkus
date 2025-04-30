package io.quarkus.runtime;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.ObserverMethod;

/**
 * This annotation is used to mark a business method of a CDI bean that should be executed during application shutdown. The
 * annotated method must be non-private and non-static and declare no arguments.
 * <p>
 * The behavior is similar to a declaration of a {@link ShutdownEvent} observer. In fact, a synthetic observer of the
 * {@link ShutdownEvent} is generated for each occurence of this annotation. Within the observer, the contextual instance of a
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
 *     void onShutdown(&#064;Observes ShutdownEvent event) {
 *         // place the logic here
 *     }
 * }
 *
 * &#064;ApplicationScoped
 * class Bean2 {
 *
 *     &#064;Shutdown
 *     void shutdown() {
 *         // place the logic here
 *     }
 * }
 * </pre>
 *
 * @see ShutdownEvent
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface Shutdown {

    /**
     *
     * @return the priority
     * @see jakarta.annotation.Priority
     */
    int value() default ObserverMethod.DEFAULT_PRIORITY;

}
