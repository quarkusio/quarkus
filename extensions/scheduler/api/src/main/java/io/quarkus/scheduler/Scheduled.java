package io.quarkus.scheduler;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.PROCEED;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled.Schedules;

/**
 * Identifies a method of a bean class that is automatically scheduled and invoked by the container.
 * <p>
 * A scheduled method is a non-abstract non-private method of a bean class. It may be either static or non-static.
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
 * The annotated method must return {@code void}, {@code java.util.concurrent.CompletionStage<Void>} or
 * {@code io.smallrye.mutiny.Uni<Void>} and either declare no parameters or one parameter of type
 * {@link ScheduledExecution}.
 * <p>
 * By default, a scheduled method is executed on the main executor for blocking tasks. However, a scheduled method that returns
 * {@code java.util.concurrent.CompletionStage<Void>} or {@code io.smallrye.mutiny.Uni<Void>}, or is annotated with
 * {@link io.smallrye.common.annotation.NonBlocking} is executed on the event loop.
 *
 * @see ScheduledExecution
 */
@Target(METHOD)
@Retention(RUNTIME)
@Repeatable(Schedules.class)
public @interface Scheduled {

    /**
     * Optionally defines a unique identifier for this job.
     * <p>
     * The value can be a property expression. In this case, the scheduler attempts to use the configured value instead:
     * {@code @Scheduled(identity = "${myJob.identity}")}.
     * Additionally, the property expression can specify a default value: {@code @Scheduled(identity =
     * "${myJob.identity:defaultIdentity}")}.
     * <p>
     * If the value is not provided then a unique id is generated.
     *
     * @return the unique identity of the schedule
     */
    String identity() default "";

    /**
     * Defines a cron-like expression. For example "0 15 10 * * ?" fires at 10:15am every day.
     * <p>
     * The value can be a property expression. In this case, the scheduler attempts to use the configured value instead:
     * {@code @Scheduled(cron = "${myJob.cronExpression}")}.
     * Additionally, the property expression can specify a default value: {@code @Scheduled(cron = "${myJob.cronExpression:0/2 *
     * * * * ?}")}.
     * <p>
     * Furthermore, two special constants can be used to disable the scheduled method: {@code off} and {@code disabled}. For
     * example, {@code @Scheduled(cron="${myJob.cronExpression:off}")} means that if the property is undefined then
     * the method is never executed.
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
     * The value can be a property expression. In this case, the scheduler attempts to use the configured value instead:
     * {@code @Scheduled(every = "${myJob.everyExpression}")}.
     * Additionally, the property expression can specify a default value: {@code @Scheduled(every =
     * "${myJob.everyExpression:5m}")}.
     * <p>
     * Furthermore, two special constants can be used to disable the scheduled method: {@code off} and {@code disabled}. For
     * example, {@code @Scheduled(every="${myJob.everyExpression:off}")} means that if the property is undefined then
     * the method is never executed.
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
     * set to a value greater than zero the value of {@link #delayed()} is ignored.
     * <p>
     * The value is parsed with {@link Duration#parse(CharSequence)}. However, if an expression starts with a digit, "PT" prefix
     * is added automatically, so for example, {@code 15s} can be used instead of {@code PT15S} and is parsed as "15 seconds".
     * Note that the absolute value of the value is always used.
     * <p>
     * The value can be a property expression. In this case, the scheduler attempts to use the configured value instead:
     * {@code @Scheduled(delayed = "${myJob.delayedExpression}")}.
     * Additionally, the property expression can specify a default value: {@code @Scheduled(delayed =
     * "${myJob.delayedExpression:5m}")}.
     *
     * @return the period expression based on the ISO-8601 duration format {@code PnDTnHnMn.nS}
     */
    String delayed() default "";

    /**
     * Specify the strategy to handle concurrent execution of a scheduled method. By default, a scheduled method can be executed
     * concurrently.
     *
     * @return the concurrent execution strategy
     */
    ConcurrentExecution concurrentExecution() default PROCEED;

    /**
     * Specify the bean class that can be used to skip any execution of a scheduled method.
     * <p>
     * There must be exactly one bean that has the specified class in its set of bean types, otherwise the build
     * fails. Furthermore, the scope of the bean must be active during execution. If the scope is {@link Dependent} then the
     * bean instance belongs exclusively to the specific scheduled method and is destroyed when the application is shut down.
     *
     * @return the bean class
     */
    Class<? extends SkipPredicate> skipExecutionIf() default Never.class;

    /**
     * Defines a period after which the job is considered overdue.
     * <p>
     * The value is parsed with {@link Duration#parse(CharSequence)}. However, if an expression starts with a digit, "PT" prefix
     * is added automatically, so for example, {@code 15m} can be used instead of {@code PT15M} and is parsed as "15 minutes".
     * Note that the absolute value of the value is always used.
     * <p>
     * The value can be a property expression. In this case, the scheduler attempts to use the configured value instead:
     * {@code @Scheduled(overdueGracePeriod = "${myJob.overdueExpression}")}.
     * Additionally, the property expression can specify a default value: {@code @Scheduled(overdueGracePeriod =
     * "${myJob.overdueExpression:5m}")}.
     *
     * @return the period expression based on the ISO-8601 duration format {@code PnDTnHnMn.nS}
     */
    String overdueGracePeriod() default "";

    @Retention(RUNTIME)
    @Target(METHOD)
    @interface Schedules {

        Scheduled[] value();

    }

    /**
     * Represents a strategy to handle concurrent execution of a scheduled method.
     * <p>
     * Note that this strategy only considers executions within the same application instance. It's not intended to work
     * across the cluster.
     */
    enum ConcurrentExecution {

        /**
         * The scheduled method can be executed concurrently, i.e. it is executed every time the trigger is fired.
         */
        PROCEED,

        /**
         * The scheduled method is never executed concurrently, i.e. a method execution is skipped until the previous
         * invocation completes.
         */
        SKIP,

    }

    /**
     *
     * @see Scheduled#skipExecutionIf()
     */
    interface SkipPredicate {

        /**
         *
         * @param execution
         * @return {@code true} if the given execution should be skipped, {@code false} otherwise
         */
        boolean test(ScheduledExecution execution);

    }

    /**
     * Execution is never skipped.
     */
    class Never implements SkipPredicate {

        @Override
        public boolean test(ScheduledExecution execution) {
            return false;
        }

    }

    /**
     * Execution is skipped if the application is not running (either not started or already shutdown).
     */
    @Singleton
    class ApplicationNotRunning implements SkipPredicate {

        private volatile boolean running;

        void started(@Observes StartupEvent event) {
            this.running = true;
        }

        void shutdown(@Observes ShutdownEvent event) {
            this.running = false;
        }

        @Override
        public boolean test(ScheduledExecution execution) {
            return !running;
        }

    }

}
