package io.quarkus.scheduler;

/**
 * This event is fired synchronously and asynchronously when an execution of a scheduled method starts.
 */
public class StartedExecution {

    private final ScheduledExecution execution;

    public StartedExecution(ScheduledExecution execution) {
        this.execution = execution;
    }

    public ScheduledExecution getExecution() {
        return execution;
    }

    @Override
    public String toString() {
        return "Started execution of [" +
                execution.getTrigger().getId() +
                "]";
    }

}
