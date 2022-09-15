package io.quarkus.runtime;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.ObserverMethod;

/**
 * This annotation can be used to initialize a CDI bean at application startup:
 * <ul>
 * <li>If a bean class is annotated then a contextual instance is created and the {@link jakarta.annotation.PostConstruct}
 * callbacks are invoked.</li>
 * <li>If a producer method is annotated then a contextual instance is created, i.e. the producer method is invoked.</li>
 * <li>If a producer field is annotated then a contextual instance is created, i.e. the producer field is read.</li>
 * <li>If a non-static non-producer no-args method of a bean class is annotated then a contextual instance is created, the
 * lifecycle callbacks are invoked and finally the method itself is invoked.</li>
 * <p>
 * The behavior is similar to a declaration of a {@link StartupEvent} observer. In fact, a synthetic observer of the
 * {@link StartupEvent} is generated for each occurence of this annotation.
 * <p>
 * Furthermore, {@link #value()} can be used to specify the priority of the generated observer method and thus affects observers
 * ordering.
 * <p>
 * The contextual instance is destroyed immediately afterwards for {@link Dependent} beans.
 * <p>
 * The following examples are functionally equivalent.
 *
 * <pre>
 * &#064;ApplicationScoped
 * class Bean1 {
 *     void onStart(&#064;Observes StartupEvent event) {
 *         // place the logic here
 *     }
 * }
 *
 * &#064;Startup
 * &#064;ApplicationScoped
 * class Bean2 {
 *
 *     &#064;PostConstruct
 *     void init() {
 *         // place the logic here
 *     }
 * }
 *
 * &#064;ApplicationScoped
 * class Bean3 {
 *
 *     &#064;Startup
 *     void init() {
 *         // place the logic here
 *     }
 * }
 * </pre>
 *
 * @see StartupEvent
 */
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
public @interface Startup {

    /**
     *
     * @return the priority
     * @see jakarta.annotation.Priority
     */
    int value() default ObserverMethod.DEFAULT_PRIORITY;

}
