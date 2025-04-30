package io.quarkus.scheduler;

/**
 * This event is fired synchronously and asynchronously when an execution of a scheduled method is delayed.
 */
public class DelayedExecution {

    private final ScheduledExecution execution;
    private final long delay;

    public DelayedExecution(ScheduledExecution execution, long delay) {
        this.execution = execution;
        this.delay = delay;
    }

    public ScheduledExecution getExecution() {
        return execution;
    }

    /**
     *
     * @return the delay in milliseconds
     */
    public long getDelay() {
        return delay;
    }

    @Override
    public String toString() {
        return "Delayed execution of [" +
                execution.getTrigger().getId() +
                "]";
    }

}
