package io.quarkus.quartz;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.SkippedExecution;

/**
 * A scheduled method annotated with this annotation may not be executed concurrently. The behavior is identical to a
 * {@link Job} class annotated with {@link DisallowConcurrentExecution}.
 * <p>
 * If {@code quarkus.quartz.run-blocking-scheduled-method-on-quartz-thread} is set to
 * {@code false} the execution of a scheduled method is offloaded to a specific Quarkus thread pool but the triggering Quartz
 * thread is blocked until the execution is finished. Therefore, make sure the Quartz thread pool is configured appropriately.
 * <p>
 * If {@code quarkus.quartz.run-blocking-scheduled-method-on-quartz-thread} is set to {@code true} the scheduled method is
 * invoked on a thread managed by Quartz.
 * <p>
 * Unlike with {@link Scheduled.ConcurrentExecution#SKIP} the {@link SkippedExecution} event is never fired if a method
 * execution is skipped by Quartz.
 *
 * @see DisallowConcurrentExecution
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface Nonconcurrent {

}
