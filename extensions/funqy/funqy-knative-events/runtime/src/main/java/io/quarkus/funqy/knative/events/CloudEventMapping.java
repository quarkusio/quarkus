package io.quarkus.funqy.knative.events;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes input and output cloud events for function for Knative Cloud Events
 * Applied to a @Funq method
 *
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CloudEventMapping {
    /**
     * Defines the cloud event type that will trigger the function
     *
     * Defaults to function name
     *
     * @return
     */
    String trigger() default "";

    /**
     * If the function has output, this describes the cloud event source of the output event
     *
     * Defaults to function name
     *
     * @return
     */
    String responseSource() default "";

    /**
     * If the function has output, this describes the cloud event type of the output event
     * Defaults to {function}.output
     * 
     * @return
     */
    String responseType() default "";

    /**
     * If there is an extra requirement to match against cloud event attributes to find the function to be triggered
     * 
     * @return
     */
    EventAttribute[] attributes() default {};
}
