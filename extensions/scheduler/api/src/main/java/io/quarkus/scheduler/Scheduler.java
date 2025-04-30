package io.quarkus.scheduler;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.context.Dependent;

import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.quarkus.scheduler.Scheduled.SkipPredicate;
import io.smallrye.mutiny.Uni;

/**
 * The container provides a built-in bean with bean type {@link Scheduler} and qualifier
 * {@link jakarta.enterprise.inject.Default}.
 */
public interface Scheduler {

    /**
     * By default, the scheduler is not started unless a {@link Scheduled} business method is discovered. However, it is
     * possible to set the {@code forced} start mode with the {@code quarkus.scheduler.start-mode} configuration property. In
     * this case, the scheduler will be started even if no scheduled business methods are found.
     *
     * @return {@code true} if the scheduler was started, {@code false} otherwise
     */
    boolean isStarted();

    /**
     * Pause the scheduler. No triggers are fired when a scheduler is paused.
     *
     * @throws UnsupportedOperationException If the scheduler was not started
     */
    void pause();

    /**
     * Pause a specific job. Identity must not be null and non-existent identity results in no-op.
     *
     * @param identity
     * @see Scheduled#identity()
     * @throws UnsupportedOperationException If the scheduler was not started
     */
    void pause(String identity);

    /**
     * Resume the scheduler. Triggers can be fired again.
     *
     * @throws UnsupportedOperationException If the scheduler was not started
     */
    void resume();

    /**
     * Resume a specific job. Identity must not be null and non-existent identity results in no-op.
     *
     * @param identity
     * @see Scheduled#identity()
     * @throws UnsupportedOperationException If the scheduler was not started
     */
    void resume(String identity);

    /**
     * Identity must not be null and {@code false} is returned for non-existent identity.
     * <p>
     * Note that this method only returns {@code true} if the job was explicitly paused. I.e. it does not reflect a paused
     * scheduler.
     *
     * @param identity
     * @return {@code true} if the job with the given identity is paused, {@code false} otherwise
     * @see Scheduled#identity()
     * @see #pause(String)
     * @throws UnsupportedOperationException If the scheduler was not started
     */
    boolean isPaused(String identity);

    /**
     * @return {@code true} if a scheduler is started the triggers are fired and jobs are executed, {@code false} otherwise
     * @see #pause()
     * @see #resume()
     */
    boolean isRunning();

    /**
     *
     * @return an immutable list of scheduled jobs represented by their trigger
     * @throws UnsupportedOperationException If the scheduler was not started
     */
    List<Trigger> getScheduledJobs();

    /**
     *
     * @return the trigger of a specific job or null for non-existent identity
     * @throws UnsupportedOperationException If the scheduler was not started
     */
    Trigger getScheduledJob(String identity);

    /**
     * Creates a new job definition. The job is not scheduled until the {@link JobDefinition#schedule()} method is called.
     * <p>
     * The properties of the job definition have the same semantics as their equivalents in the {@link Scheduled}
     * annotation.
     *
     * @param identity The identity must be unique for the scheduler
     * @return a new job definition
     * @see Scheduled#identity()
     * @throws UnsupportedOperationException If the scheduler was not started
     */
    JobDefinition<?> newJob(String identity);

    /**
     * Removes the job previously added via {@link #newJob(String)}.
     * <p>
     * It is a no-op if the identified job was not added programmatically.
     *
     * @param identity
     * @return the trigger or {@code null} if no such job exists
     * @throws UnsupportedOperationException If the scheduler was not started
     */
    Trigger unscheduleJob(String identity);

    /**
     *
     * @return the implementation
     * @see Scheduled#executeWith()
     */
    String implementation();

    /**
     * The job definition is a builder-like API that can be used to define a job programmatically.
     * <p>
     * No job is scheduled until the {@link #setTask(Consumer)} or {@link #setAsyncTask(Function)} method is called.
     * <p>
     * The implementation is not thread-safe and should not be reused.
     */
    interface JobDefinition<THIS extends JobDefinition<THIS>> {

        /**
         * The schedule is defined either by {@link #setCron(String)} or by {@link #setInterval(String)}. If both methods are
         * used, then the cron expression takes precedence.
         * <p>
         * {@link Scheduled#cron()}
         *
         * @param cron
         * @return self
         * @see Scheduled#cron()
         */
        THIS setCron(String cron);

        /**
         * The schedule is defined either by {@link #setCron(String)} or by {@link #setInterval(String)}. If both methods are
         * used, then the cron expression takes precedence.
         * <p>
         * A value less than one second may not be supported by the underlying scheduler implementation. In that case a warning
         * message is logged immediately.
         * <p>
         * {@link Scheduled#every()}
         *
         * @param every
         * @return self
         * @see Scheduled#every()
         */
        THIS setInterval(String every);

        /**
         * {@link Scheduled#delayed()}
         *
         * @param period
         * @return self
         * @see Scheduled#delayed()
         */
        THIS setDelayed(String period);

        /**
         * {@link Scheduled#concurrentExecution()}
         *
         * @param concurrentExecution
         * @return self
         * @see Scheduled#concurrentExecution()
         */
        THIS setConcurrentExecution(ConcurrentExecution concurrentExecution);

        /**
         * {@link Scheduled#skipExecutionIf()}
         *
         * @param skipPredicate
         * @return self
         * @see Scheduled#skipExecutionIf()
         */
        THIS setSkipPredicate(SkipPredicate skipPredicate);

        /**
         * {@link Scheduled#skipExecutionIf()}
         *
         * @param skipPredicateClass
         * @return self
         * @see Scheduled#skipExecutionIf()
         */
        THIS setSkipPredicate(Class<? extends SkipPredicate> skipPredicateClass);

        /**
         * {@link Scheduled#overdueGracePeriod()}
         *
         * @param period
         * @return self
         * @see Scheduled#overdueGracePeriod()
         */
        THIS setOverdueGracePeriod(String period);

        /**
         * {@link Scheduled#timeZone()}
         *
         * @return self
         * @see Scheduled#timeZone()
         */
        THIS setTimeZone(String timeZone);

        /**
         * {@link Scheduled#executeWith()}
         *
         * @param implementation
         * @return self
         * @throws IllegalArgumentException If the composite scheduler is used and the selected implementation is not available
         * @see Scheduled#executeWith()
         */
        THIS setExecuteWith(String implementation);

        /**
         * {@link Scheduled#executionMaxDelay()}
         *
         * @param maxDelay
         * @return self
         * @see Scheduled#executionMaxDelay()
         */
        THIS setExecutionMaxDelay(String maxDelay);

        /**
         *
         * @param task
         * @return self
         */
        default THIS setTask(Consumer<ScheduledExecution> task) {
            return setTask(task, false);
        }

        /**
         * The class must either represent a CDI bean or declare a public no-args constructor.
         * <p>
         * In case of CDI, there must be exactly one bean that has the specified class in its set of bean types. The scope of
         * the bean must be active during execution of the job. If the scope is {@link Dependent} then the bean instance belongs
         * exclusively to the specific job definition and is destroyed when the application is shut down. If the bean is not a
         * dependency of any other bean it has to be marked as unremovable; for example annotated with
         * {@link io.quarkus.arc.Unremovable}.
         * <p>
         * In case of a class with public no-args constructor, the constructor must be registered for reflection when an
         * application is compiled to a native executable; for example annotate the class with
         * {@link io.quarkus.runtime.annotations.RegisterForReflection}.
         *
         * @param taskClass
         * @return self
         */
        default THIS setTask(Class<? extends Consumer<ScheduledExecution>> taskClass) {
            return setTask(taskClass, false);
        }

        /**
         * Configures the task to schedule.
         *
         * @param task the task, must not be {@code null}
         * @param runOnVirtualThread whether the task must be run on a virtual thread if the JVM allows it.
         * @return self
         */
        THIS setTask(Consumer<ScheduledExecution> task, boolean runOnVirtualThread);

        /**
         * The class must either represent a CDI bean or declare a public no-args constructor.
         * <p>
         * In case of CDI, there must be exactly one bean that has the specified class in its set of bean types. The scope of
         * the bean must be active during execution of the job. If the scope is {@link Dependent} then the bean instance belongs
         * exclusively to the specific job definition and is destroyed when the application is shut down. If the bean is not a
         * dependency of any other bean it has to be marked as unremovable; for example annotated with
         * {@link io.quarkus.arc.Unremovable}.
         * <p>
         * In case of a class with public no-args constructor, the constructor must be registered for reflection when an
         * application is compiled to a native executable; for example annotate the class with
         * {@link io.quarkus.runtime.annotations.RegisterForReflection}.
         *
         * @param consumerClass
         * @param runOnVirtualThread
         * @return self
         */
        THIS setTask(Class<? extends Consumer<ScheduledExecution>> consumerClass, boolean runOnVirtualThread);

        /**
         *
         * @param asyncTask
         * @return self
         */
        THIS setAsyncTask(Function<ScheduledExecution, Uni<Void>> asyncTask);

        /**
         * The class must either represent a CDI bean or declare a public no-args constructor.
         * <p>
         * In case of CDI, there must be exactly one bean that has the specified class in its set of bean types. The scope of
         * the bean must be active during execution of the job. If the scope is {@link Dependent} then the bean instance belongs
         * exclusively to the specific job definition and is destroyed when the application is shut down. If the bean is not a
         * dependency of any other bean it has to be marked as unremovable; for example annotated with
         * {@link io.quarkus.arc.Unremovable}.
         * <p>
         * In case of a class with public no-args constructor, the constructor must be registered for reflection when an
         * application is compiled to a native executable; for example annotate the class with
         * {@link io.quarkus.runtime.annotations.RegisterForReflection}.
         *
         * @param asyncTaskClass
         * @return self
         */
        THIS setAsyncTask(Class<? extends Function<ScheduledExecution, Uni<Void>>> asyncTaskClass);

        /**
         * Attempts to schedule the job.
         *
         * @return the trigger
         */
        Trigger schedule();
    }
}
