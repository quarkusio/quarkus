package io.quarkus.scheduler;

/**
 * This event is fired synchronously and asynchronously when the {@link Scheduler#resume(String)} method is called.
 */
public class ScheduledJobResumed {

    private final Trigger trigger;

    public ScheduledJobResumed(Trigger trigger) {
        this.trigger = trigger;
    }

    public Trigger getTrigger() {
        return trigger;
    }

}
