package io.quarkus.scheduler;

/**
 * This event is fired synchronously and asynchronously when the {@link Scheduler#resume()} method is called.
 */
public class SchedulerResumed {

    public static final SchedulerResumed INSTANCE = new SchedulerResumed();

}
