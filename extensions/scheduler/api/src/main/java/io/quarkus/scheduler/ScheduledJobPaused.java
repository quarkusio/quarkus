package io.quarkus.scheduler;

/**
 * This event is fired synchronously and asynchronously when the {@link Scheduler#pause(String)} method is called.
 */
public class ScheduledJobPaused {

    private final Trigger trigger;

    public ScheduledJobPaused(Trigger trigger) {
        this.trigger = trigger;
    }

    public Trigger getTrigger() {
        return trigger;
    }

}
