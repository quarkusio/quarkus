package io.quarkus.runtime;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.ObserverMethod;

/**
 * This annotation can be used to initialize a CDI bean at application startup. The behavior is similar to a declaration of an
 * observer of the {@link StartupEvent} - a contextual instance is created and lifecycle callbacks (such as
 * {@link javax.annotation.PostConstruct}) are invoked. In fact, a synthetic observer of the {@link StartupEvent} is generated
 * for each bean annotated with this annotation. Furthermore, {@link #value()} can be used to specify the priority of the
 * generated observer method and thus affect observers ordering.
 * <p>
 * The contextual instance is destroyed immediately afterwards for {@link Dependent} beans.
 * <p>
 * The following examples are functionally equivalent.
 * 
 * <pre>
 * &#064;ApplicationScoped
 * class Bean1 {
 *     void onStart(@Observes StartupEvent event) {
 *         // place the logic here 
 *     }
 * }
 * 
 * &#064;Startup
 * &#064;ApplicationScoped
 * class Bean2 {
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
     * @see javax.annotation.Priority
     */
    int value() default ObserverMethod.DEFAULT_PRIORITY;

}
