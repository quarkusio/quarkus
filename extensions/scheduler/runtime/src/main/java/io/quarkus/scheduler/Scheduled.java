package io.quarkus.scheduler;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.quarkus.scheduler.Scheduled.Schedules;

/**
 * Marks a business method to be automatically scheduled and invoked by the container.
 * <p>
 * The schedule is defined either by {@link #cron()} or by {@link #every()} attribute. If both are specified, the cron
 * expression takes precedence.
 *
 * <pre>
 * &#64;ApplicationScoped
 * class MyService {
 *
 *     &#64;Scheduled(cron = "0/5 * * * * ?")
 *     void check() {
 *         // do something important every 5 seconds
 *     }
 * }
 * </pre>
 *
 * The annotated method must return {@code void} and either declare no parameters or one parameter of type
 * {@link ScheduledExecution}.
 *
 * @author Martin Kouba
 * @see ScheduledExecution
 */
@Target(METHOD)
@Retention(RUNTIME)
@Repeatable(Schedules.class)
public @interface Scheduled {

    /**
     * Optionally defines a unique identifier for this job.
     * <p>
     * If the value is not given, Quarkus will generate a unique id.
     * <p>
     * 
     * @return the unique identity of the schedule
     */
    String identity() default "";

    /**
     * Defines a cron-like expression. For example "0 15 10 * * ?" fires at 10:15am every day.
     * <p>
     * If the value starts with "&#123;" and ends with "&#125;" the scheduler attempts to find a corresponding config property
     * and use the configured value instead: {@code &#64;Scheduled(cron = "{myservice.check.cron.expr}")}.
     *
     * @return the cron-like expression
     */
    String cron() default "";

    /**
     * Defines a period between invocations.
     * <p>
     * The value is parsed with {@link Duration#parse(CharSequence)}. However, if an expression starts with a digit, "PT" prefix
     * is added automatically, so for example, {@code 15m} can be used instead of {@code PT15M} and is parsed as "15 minutes".
     * Note that the absolute value of the value is always used.
     * <p>
     * If the value starts with "&#123;" and ends with "&#125;" the scheduler attempts to find a corresponding config property
     * and use the configured value instead: {@code &#64;Scheduled(every = "{myservice.check.every.expr}")}.
     *
     * @return the period expression based on the ISO-8601 duration format {@code PnDTnHnMn.nS}
     */
    String every() default "";

    /**
     * Delays the time the trigger should start at. The value is rounded to full second.
     * <p>
     * By default, the trigger starts when registered.
     *
     * @return the initial delay
     */
    long delay() default 0;

    /**
     * 
     * @return the unit of initial delay
     * @see Scheduled#delay()
     */
    TimeUnit delayUnit() default TimeUnit.MINUTES;

    /**
     * Defines a period after which the trigger should start. It's an alternative to {@link #delay()}. If {@link #delay()} is
     * set to a value greater then zero the value of {@link #delayed()} is ignored.
     * <p>
     * The value is parsed with {@link Duration#parse(CharSequence)}. However, if an expression starts with a digit, "PT" prefix
     * is added automatically, so for example, {@code 15s} can be used instead of {@code PT15S} and is parsed as "15 seconds".
     * Note that the absolute value of the value is always used.
     * <p>
     * If the value starts with "&#123;" and ends with "&#125;" the scheduler attempts to find a corresponding config property
     * and use the configured value instead: {@code &#64;Scheduled(delayed = "{myservice.delayed}")}.
     *
     * @return the period expression based on the ISO-8601 duration format {@code PnDTnHnMn.nS}
     */
    String delayed() default "";

    @Retention(RUNTIME)
    @Target(METHOD)
    @interface Schedules {

        Scheduled[] value();

    }

}
